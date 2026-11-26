package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricsService")
class MetricsServiceTest {

  @Test
  @DisplayName("Should count only 500-599 statuses when nothing threw")
  void isServerError_WhenNothingThrew_ShouldCountOnlyFiveHundredRange() {
    assertThat(MetricsService.isServerError(200, false, false)).isFalse();
    assertThat(MetricsService.isServerError(499, false, false)).isFalse();
    assertThat(MetricsService.isServerError(500, false, false)).isTrue();
    assertThat(MetricsService.isServerError(599, false, false)).isTrue();
    // The exception handlers treat only 500-599 as server errors.
    assertThat(MetricsService.isServerError(600, false, false)).isFalse();
  }

  @Test
  @DisplayName("Should count a throw when no status was sent yet")
  void isServerError_WhenThrewBeforeCommit_ShouldBeTrue() {
    // The container answers 500, whatever status the response object still holds.
    assertThat(MetricsService.isServerError(200, true, false)).isTrue();
  }

  @Test
  @DisplayName("Should let the sent status decide when the throw came after commit")
  void isServerError_WhenThrewAfterCommit_ShouldUseSentStatus() {
    // A client that hung up mid-body after a 200 is not a server error.
    assertThat(MetricsService.isServerError(200, true, true)).isFalse();
    assertThat(MetricsService.isServerError(500, true, true)).isTrue();
  }
}
