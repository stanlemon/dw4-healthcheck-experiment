package com.stanlemon.healthy.metrics;

/**
 * Tracks request outcomes in a 60-second sliding window and reports them as one consistent {@link
 * MetricsSnapshot}.
 *
 * <p>Implementations must be thread-safe: requests are recorded from many request threads while
 * health checks read snapshots concurrently.
 *
 * <p>Intended to be wired as a singleton by the host framework (Dropwizard or Spring Boot in this
 * project). Consumers may store references without defensive copying — the shared instance is the
 * design.
 */
public interface MetricsService {

  /**
   * Record one finished request. The latency and the error flag are recorded together, so a reader
   * never sees an error without the request it belongs to.
   *
   * @param latencyMs how long the request took, in milliseconds (negative values count as 0)
   * @param serverError true if the request ended in a 5xx response
   */
  void recordRequest(long latencyMs, boolean serverError);

  /**
   * Read every metric and threshold check at one moment.
   *
   * @return a snapshot whose fields all describe the same moment
   */
  MetricsSnapshot snapshot();
}
