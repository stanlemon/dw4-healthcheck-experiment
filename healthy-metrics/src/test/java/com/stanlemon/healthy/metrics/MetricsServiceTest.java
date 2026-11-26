package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("MetricsService")
class MetricsServiceTest {

  @ParameterizedTest(name = "status {0}, threw {1}, committed {2} -> server error {3}")
  @CsvSource({
    "200, false, false, false",
    "499, false, false, false",
    "500, false, false, true",
    "599, false, false, true",
    // The exception handlers treat only 500-599 as server errors.
    "600, false, false, false",
    // Threw before a status went out: the container answers 500.
    "200, true,  false, true",
    // Threw after a 200 went out (the client hung up mid-body): the sent status decides.
    "200, true,  true,  false",
    "500, true,  true,  true"
  })
  @DisplayName("Should count only 5xx statuses and throws before the response was committed")
  void isServerError_WhenGivenOutcome_ShouldMatchRule(
      int status, boolean threw, boolean committed, boolean expected) {
    assertThat(MetricsService.isServerError(status, threw, committed)).isEqualTo(expected);
  }
}
