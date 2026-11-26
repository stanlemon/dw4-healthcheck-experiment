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
   * @param serverError true if the request ended in a 5xx response; see {@link #isServerError}
   */
  void recordRequest(long latencyMs, boolean serverError);

  /**
   * Decides whether a finished request counts as a server error. Both apps' request filters use
   * this, so the rule lives in one place.
   *
   * <p>A request is a server error if the client got a 5xx status, or if handling threw before any
   * status was sent (the container then answers 500). A throw after the response was committed
   * means the status already went out, usually because the client hung up mid-body, so the sent
   * status decides.
   *
   * @param status the response status the request ended with
   * @param threw true if handling the request threw an exception
   * @param committed true if the status and headers were already sent to the client
   * @return true if the request should be recorded as a server error
   */
  static boolean isServerError(int status, boolean threw, boolean committed) {
    return (threw && !committed) || (status >= 500 && status < 600);
  }

  /**
   * Read every metric and threshold check at one moment.
   *
   * @return a snapshot whose fields all describe the same moment
   */
  MetricsSnapshot snapshot();
}
