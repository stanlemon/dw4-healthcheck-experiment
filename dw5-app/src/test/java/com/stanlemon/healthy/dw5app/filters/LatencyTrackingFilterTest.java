package com.stanlemon.healthy.dw5app.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsSnapshot;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LatencyTrackingFilter")
class LatencyTrackingFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain chain;

  private DefaultMetricsService metricsService;
  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    filter = new LatencyTrackingFilter(metricsService);
  }

  private void runFilter(String uri, int status) throws ServletException, IOException {
    when(request.getRequestURI()).thenReturn(uri);
    when(response.getStatus()).thenReturn(status);
    filter.doFilter(request, response, chain);
  }

  @Test
  @DisplayName("Should record request without error when response is ok")
  void doFilter_WhenResponseIsOk_ShouldRecordRequestWithoutError()
      throws ServletException, IOException {
    runFilter("/hangar/planes", 200);

    verify(chain).doFilter(request, response);
    MetricsSnapshot snapshot = metricsService.snapshot();
    assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
    assertThat(snapshot.getErrorsLastMinute()).isZero();
    // The mocked chain returns at once; a seconds-vs-milliseconds bug would push this past 1000.
    assertThat(snapshot.getAverageLatencyLast60Seconds()).isBetween(0.0, 1000.0);
  }

  @Test
  @DisplayName("Should record error when response is server error")
  void doFilter_WhenResponseIsServerError_ShouldRecordError() throws ServletException, IOException {
    runFilter("/hangar/planes", 500);

    MetricsSnapshot snapshot = metricsService.snapshot();
    assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
    assertThat(snapshot.getErrorsLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should not record error when response is client error")
  void doFilter_WhenResponseIsClientError_ShouldNotRecordError()
      throws ServletException, IOException {
    runFilter("/hangar/planes", 404);

    assertThat(metricsService.snapshot().getErrorsLastMinute()).isZero();
  }

  @Test
  @DisplayName("Should not record error when status is above five hundred range")
  void doFilter_WhenStatusIsAboveFiveHundredRange_ShouldNotRecordError()
      throws ServletException, IOException {
    // The exception handlers treat only 500-599 as server errors; the filter must agree.
    runFilter("/hangar/planes", 600);

    assertThat(metricsService.snapshot().getErrorsLastMinute()).isZero();
  }

  @Test
  @DisplayName("Should not record error when chain throws after ok response committed")
  void doFilter_WhenChainThrowsAfterOkResponseCommitted_ShouldNotRecordError()
      throws ServletException, IOException {
    // A client that hangs up mid-body makes the write throw after a 200 already went out.
    when(request.getRequestURI()).thenReturn("/hangar/planes");
    when(response.isCommitted()).thenReturn(true);
    when(response.getStatus()).thenReturn(200);
    IOException clientAbort = new IOException("Broken pipe");
    doThrow(clientAbort).when(chain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isSameAs(clientAbort);

    MetricsSnapshot snapshot = metricsService.snapshot();
    assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
    assertThat(snapshot.getErrorsLastMinute()).isZero();
  }

  @Test
  @DisplayName("Should record error and rethrow when chain throws")
  void doFilter_WhenChainThrows_ShouldRecordErrorAndRethrow() throws ServletException, IOException {
    when(request.getRequestURI()).thenReturn("/hangar/planes");
    doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilter(request, response, chain))
        .isInstanceOf(ServletException.class)
        .hasMessage("boom");

    assertThat(metricsService.snapshot().getErrorsLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record nothing when health probe")
  void doFilter_WhenHealthProbe_ShouldRecordNothing() throws ServletException, IOException {
    when(request.getRequestURI()).thenReturn("/health/ready");

    filter.doFilter(request, response, chain);

    assertThat(metricsService.snapshot().getRequestsLast60Seconds()).isZero();
  }
}
