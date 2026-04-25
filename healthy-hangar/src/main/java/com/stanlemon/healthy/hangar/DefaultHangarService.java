package com.stanlemon.healthy.hangar;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * In-memory thread-safe implementation of {@link HangarService}. Not durable — state is lost on
 * restart — and bounded by {@link #MAX_PLANES}; when full the oldest plane is evicted.
 */
public class DefaultHangarService implements HangarService {

  static final int MAX_PLANES = 1000;

  private static final Comparator<PaperPlane> OLDEST_FIRST =
      Comparator.comparing(PaperPlane::getStowedAt).thenComparing(PaperPlane::getId);

  private final ConcurrentMap<String, PaperPlane> planes = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<String> insertionOrder = new ConcurrentLinkedQueue<>();
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
    planes.put(plane.getId(), plane);
    insertionOrder.offer(plane.getId());
    evictOldestIfOverCapacity();
    return plane;
  }

  private void evictOldestIfOverCapacity() {
    while (planes.size() > MAX_PLANES) {
      String oldestId = insertionOrder.poll();
      if (oldestId == null) {
        return;
      }
      planes.remove(oldestId);
    }
  }

  @Override
  public Optional<PaperPlane> find(String id) {
    return Optional.ofNullable(planes.get(id));
  }

  @Override
  public List<PaperPlane> listAll() {
    return planes.values().stream().sorted(OLDEST_FIRST.reversed()).toList();
  }

  @Override
  public int count() {
    return planes.size();
  }
}
