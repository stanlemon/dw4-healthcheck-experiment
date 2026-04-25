package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultHangarService")
class DefaultHangarServiceTest {

  private Clock clock;
  private AtomicInteger idCounter;
  private DefaultHangarService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-04-21T10:00:00Z"), ZoneOffset.UTC);
    idCounter = new AtomicInteger();
    service = new DefaultHangarService(clock, () -> "id-" + idCounter.incrementAndGet());
  }

  @Nested
  @DisplayName("stow")
  class Stow {

    @Test
    @DisplayName("Assigns a generated id and the clock's timestamp")
    void stow_AssignsGeneratedIdAndTimestamp() {
      PaperPlane plane = service.stow(sampleRequest("Phoenix"));

      assertThat(plane.getId()).isEqualTo("id-1");
      assertThat(plane.getStowedAt()).isEqualTo(Instant.parse("2026-04-21T10:00:00Z"));
      assertThat(plane.getName()).isEqualTo("Phoenix");
    }

    @Test
    @DisplayName("Copies all request fields onto the stored plane")
    void stow_CopiesRequestFields() {
      PaperPlaneRequest request = new PaperPlaneRequest("Glider", 30.5, 90, NoseStyle.POINTED);

      PaperPlane plane = service.stow(request);

      assertThat(plane.getWingspanCm()).isEqualTo(30.5);
      assertThat(plane.getPaperGsm()).isEqualTo(90);
      assertThat(plane.getNoseStyle()).isEqualTo(NoseStyle.POINTED);
    }

    @Test
    @DisplayName("Generates a distinct id for each stowed plane")
    void stow_GeneratesDistinctIds() {
      PaperPlane first = service.stow(sampleRequest("one"));
      PaperPlane second = service.stow(sampleRequest("two"));

      assertThat(first.getId()).isNotEqualTo(second.getId());
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("Returns a previously stowed plane by id")
    void find_ReturnsStowedPlane() {
      PaperPlane stowed = service.stow(sampleRequest("Kestrel"));

      assertThat(service.find(stowed.getId())).contains(stowed);
    }

    @Test
    @DisplayName("Returns empty for an unknown id when other planes exist")
    void find_ReturnsEmptyForUnknownId() {
      service.stow(sampleRequest("existing"));

      assertThat(service.find("does-not-exist")).isEmpty();
    }
  }

  @Nested
  @DisplayName("listAll")
  class ListAll {

    @Test
    @DisplayName("Is empty on a fresh hangar")
    void listAll_IsEmptyOnFreshHangar() {
      assertThat(service.listAll()).isEmpty();
    }

    @Test
    @DisplayName("Orders planes by stowedAt descending")
    void listAll_OrdersMostRecentFirst() {
      AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-04-21T10:00:00Z"));
      AtomicInteger ids = new AtomicInteger();
      DefaultHangarService svc =
          new DefaultHangarService(fixedSource(now), () -> "id-" + ids.incrementAndGet());

      PaperPlane p1 = svc.stow(sampleRequest("earliest"));
      now.set(Instant.parse("2026-04-21T11:00:00Z"));
      PaperPlane p2 = svc.stow(sampleRequest("middle"));
      now.set(Instant.parse("2026-04-21T12:00:00Z"));
      PaperPlane p3 = svc.stow(sampleRequest("latest"));

      assertThat(svc.listAll()).containsExactly(p3, p2, p1);
    }
  }

  @Test
  @DisplayName("count reflects the number of stowed planes")
  void count_ReflectsStowedPlanes() {
    assertThat(service.count()).isZero();

    service.stow(sampleRequest("a"));
    service.stow(sampleRequest("b"));

    assertThat(service.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("stow evicts the oldest plane once capacity is exceeded")
  void stow_EvictsOldestWhenOverCapacity() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-04-21T10:00:00Z"));
    AtomicInteger ids = new AtomicInteger();
    DefaultHangarService bounded =
        new DefaultHangarService(fixedSource(now), () -> "id-" + ids.incrementAndGet());

    PaperPlane oldest = bounded.stow(sampleRequest("oldest"));
    for (int i = 0; i < DefaultHangarService.MAX_PLANES; i++) {
      now.set(now.get().plusSeconds(1));
      bounded.stow(sampleRequest("p" + i));
    }

    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);
    assertThat(bounded.find(oldest.getId())).isEmpty();
  }

  @Test
  @DisplayName("listAll breaks stowedAt ties by id so ordering is deterministic")
  void listAll_BreaksTiesOnId() {
    AtomicInteger ids = new AtomicInteger();
    DefaultHangarService svc =
        new DefaultHangarService(clock, () -> String.format("id-%03d", ids.incrementAndGet()));

    PaperPlane first = svc.stow(sampleRequest("a"));
    PaperPlane second = svc.stow(sampleRequest("b"));
    PaperPlane third = svc.stow(sampleRequest("c"));

    assertThat(svc.listAll()).containsExactly(third, second, first);
  }

  @Test
  @DisplayName("Concurrent stow calls honor the capacity cap without over-eviction")
  void stow_ConcurrentStowsPreserveCapacityCap() throws InterruptedException {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-04-21T10:00:00Z"));
    AtomicInteger ids = new AtomicInteger();
    DefaultHangarService bounded =
        new DefaultHangarService(
            fixedSource(now), () -> String.format("id-%06d", ids.incrementAndGet()));

    int threads = 8;
    int perThread = (DefaultHangarService.MAX_PLANES + 200) / threads;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);

    try {
      for (int t = 0; t < threads; t++) {
        pool.submit(
            () -> {
              try {
                start.await();
                for (int i = 0; i < perThread; i++) {
                  now.updateAndGet(prev -> prev.plusMillis(1));
                  bounded.stow(sampleRequest("p"));
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                done.countDown();
              }
            });
      }
      start.countDown();
      assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    } finally {
      pool.shutdownNow();
    }

    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);
    List<PaperPlane> all = bounded.listAll();
    assertThat(all).hasSize(DefaultHangarService.MAX_PLANES);
    for (int i = 1; i < all.size(); i++) {
      PaperPlane prev = all.get(i - 1);
      PaperPlane curr = all.get(i);
      boolean ordered =
          prev.getStowedAt().isAfter(curr.getStowedAt())
              || (prev.getStowedAt().equals(curr.getStowedAt())
                  && prev.getId().compareTo(curr.getId()) > 0);
      assertThat(ordered).isTrue();
    }
  }

  @Test
  @DisplayName("Eviction loop handles multiple over-capacity planes in one batch")
  void stow_WhenMultipleOverCapacity_ShouldEvictAllExcess() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-04-21T10:00:00Z"));
    AtomicInteger ids = new AtomicInteger();
    DefaultHangarService bounded =
        new DefaultHangarService(fixedSource(now), () -> "id-" + ids.incrementAndGet());

    // Fill to exactly capacity
    for (int i = 0; i < DefaultHangarService.MAX_PLANES; i++) {
      now.set(now.get().plusSeconds(1));
      bounded.stow(sampleRequest("p" + i));
    }
    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);

    // Add 3 more in rapid succession — each stow triggers eviction
    now.set(now.get().plusSeconds(1));
    bounded.stow(sampleRequest("extra1"));
    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);

    now.set(now.get().plusSeconds(1));
    bounded.stow(sampleRequest("extra2"));
    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);

    now.set(now.get().plusSeconds(1));
    bounded.stow(sampleRequest("extra3"));
    assertThat(bounded.count()).isEqualTo(DefaultHangarService.MAX_PLANES);
  }

  private static PaperPlaneRequest sampleRequest(String name) {
    return new PaperPlaneRequest(name, 22.0, 80, NoseStyle.POINTED);
  }

  private static Clock fixedSource(AtomicReference<Instant> now) {
    return new Clock() {
      @Override
      public ZoneOffset getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(java.time.ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return now.get();
      }
    };
  }
}
