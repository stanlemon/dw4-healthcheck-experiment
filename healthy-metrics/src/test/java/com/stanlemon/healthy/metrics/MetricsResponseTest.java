package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricsResponse")
class MetricsResponseTest {

  @Test
  @DisplayName("Should copy reported fields when given snapshot")
  void from_WhenGivenSnapshot_ShouldCopyReportedFields() {
    MetricsSnapshot snapshot = new MetricsSnapshot(3, 7, 40, 125.5, false, true, 0.5, 100.0);

    assertThat(MetricsResponse.from(snapshot))
        .isEqualTo(new MetricsResponse(3, 7, 125.5, false, true));
  }

  @Test
  @DisplayName("Should be false when either threshold breached")
  void isHealthy_WhenEitherThresholdBreached_ShouldBeFalse() {
    assertThat(new MetricsResponse(0, 0, 0.0, false, false).isHealthy()).isTrue();
    assertThat(new MetricsResponse(0, 0, 0.0, true, false).isHealthy()).isFalse();
    assertThat(new MetricsResponse(0, 0, 0.0, false, true).isHealthy()).isFalse();
  }
}
