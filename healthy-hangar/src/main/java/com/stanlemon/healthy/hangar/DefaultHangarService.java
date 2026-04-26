package com.stanlemon.healthy.hangar;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * In-memory thread-safe implementation of {@link HangarService}. Not durable — state is lost on
 * restart — and bounded by {@link #MAX_PLANES}; when full the oldest plane is evicted.
 *
 * <p>Backed by a {@link LinkedHashMap} whose {@link LinkedHashMap#removeEldestEntry} hook performs
 * eviction, so insertion order and presence stay in lockstep. All access is guarded by the map
 * instance.
 */
public class DefaultHangarService implements HangarService {

  static final int MAX_PLANES = 1000;

  private static final Comparator<PaperPlane> NEWEST_FIRST =
      Comparator.comparing(PaperPlane::getStowedAt).thenComparing(PaperPlane::getId).reversed();

  // Size the backing map for MAX_PLANES + default load factor (0.75) so we don't resize as the
  // hangar fills. (MAX_PLANES / 0.75) + 1 is the smallest bucket array that holds MAX_PLANES
  // without triggering a rehash.
  private final Map<String, PaperPlane> planes =
      new LinkedHashMap<>((int) (MAX_PLANES / 0.75f) + 1, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PaperPlane> eldest) {
          return size() > MAX_PLANES;
        }
      };

  private final Clock clock;
  private final Supplier<String> idSupplier;

  public DefaultHangarService() {
    this(Clock.systemUTC(), () -> UUID.randomUUID().toString());
  }

  public DefaultHangarService(Clock clock, Supplier<String> idSupplier) {
    this.clock = clock;
    this.idSupplier = idSupplier;
  }

  @Override
  public PaperPlane stow(PaperPlaneRequest request) {
    PaperPlane plane =
        new PaperPlane(
            idSupplier.get(),
            request.getName(),
            request.getWingspanCm(),
            request.getPaperGsm(),
            request.getNoseStyle(),
            clock.instant());
    synchronized (planes) {
      planes.put(plane.getId(), plane);
    }
    return plane;
  }

  @Override
  public Optional<PaperPlane> find(String id) {
    synchronized (planes) {
      return Optional.ofNullable(planes.get(id));
    }
  }

  @Override
  public List<PaperPlane> listAll() {
    synchronized (planes) {
      return planes.values().stream().sorted(NEWEST_FIRST).toList();
    }
  }

  @Override
  public int count() {
    synchronized (planes) {
      return planes.size();
    }
  }
}
