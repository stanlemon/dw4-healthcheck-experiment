package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LivenessResponse")
class LivenessResponseTest {

  @Test
  @DisplayName("Should be alive only when status is alive")
  void isAlive_WhenStatusIsAliveOrDead_ShouldMatchStatus() {
    assertThat(new LivenessResponse("alive", "ok", 0, 10).isAlive()).isTrue();
    assertThat(new LivenessResponse("dead", "failing", 10, 10).isAlive()).isFalse();
  }
}
