package com.example.dw.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Value;

/** Resource for testing latency thresholds by introducing artificial delays. */
@Path("/slow")
@Produces(MediaType.APPLICATION_JSON)
public class SlowResource {

  /** Response class for slow endpoint results. */
  @Value
  public static class SlowResponse {
    String message;
    long delayMs;
    long actualMs;
  }

  /**
   * Default slow endpoint that sleeps for 1 second.
   *
   * @return response with delay information
   */
  @GET
  public Response defaultSlow() {
    return slowRequest(1000);
  }

  /**
   * Parameterized slow endpoint that sleeps for the specified number of milliseconds.
   *
   * @param delayMs the number of milliseconds to sleep
   * @return response with delay information
   */
  @GET
  @Path("/{delayMs}")
  public Response slowWithDelay(@PathParam("delayMs") long delayMs) {
    // Limit delay to prevent abuse (max 10 seconds)
    if (delayMs > 10000) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new SlowResponse("Delay too long. Maximum allowed is 10000ms", delayMs, 0))
          .build();
    }

    if (delayMs < 0) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new SlowResponse("Delay cannot be negative", delayMs, 0))
          .build();
    }

    return slowRequest(delayMs);
  }

  /**
   * Helper method to perform the actual slow request.
   *
   * @param delayMs the number of milliseconds to sleep
   * @return response with delay information
   */
  private Response slowRequest(long delayMs) {
    long startTime = System.currentTimeMillis();

    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              new SlowResponse(
                  "Request was interrupted", delayMs, System.currentTimeMillis() - startTime))
          .build();
    }

    long actualMs = System.currentTimeMillis() - startTime;

    String message =
        String.format(
            "Slow request completed. Requested %dms delay, actual %dms", delayMs, actualMs);

    return Response.ok(new SlowResponse(message, delayMs, actualMs)).build();
  }
}
