# Dropwizard Application

A simple Dropwizard application running on JDK 21 with the following endpoints:

1. Hello World endpoint returning JSON
2. Error endpoint that throws a 500 error
3. Metrics endpoint showing error counts
4. Health check endpoint that monitors error rates

## Requirements

- JDK 21
- Maven

## Building the Application

```bash
mvn clean package
```

## Running the Application

```bash
java -jar target/dw-test2-1.0-SNAPSHOT.jar server config.yml
```

## Endpoints

- Hello World: [http://localhost:8097/hello](http://localhost:8097/hello) - Returns a simple JSON message
- Error Test: [http://localhost:8097/error](http://localhost:8097/error) - Deliberately throws a 500 error
- Metrics: [http://localhost:8097/metrics](http://localhost:8097/metrics) - Shows error counts and health status
- Health Check: [http://localhost:8098/healthcheck](http://localhost:8098/healthcheck) - Returns health status

### Test Error Endpoints

These endpoints demonstrate how the global exception mapper catches different types of errors:

- Runtime Exception: [http://localhost:8097/test-errors/runtime/your-message](http://localhost:8097/test-errors/runtime/your-message)
- Web Application Exception: [http://localhost:8097/test-errors/web-app/500](http://localhost:8097/test-errors/web-app/500)
- Arithmetic Exception: [http://localhost:8097/test-errors/arithmetic](http://localhost:8097/test-errors/arithmetic)
- Null Pointer Exception: [http://localhost:8097/test-errors/null-pointer](http://localhost:8097/test-errors/null-pointer)

## Health Check

The health check monitors the number of 500 errors produced by the service:

- It tracks errors in a 1-minute sliding window
- If more than 100 errors occur in the last minute, the health check returns unhealthy
- Current error counts can be viewed at the metrics endpoint

## Global Exception Handling

The application uses a global exception mapper to catch all exceptions:

- All endpoints benefit from consistent error handling
- All 5xx errors are automatically tracked for health monitoring
- Different types of exceptions are handled appropriately
- The middleware approach ensures no errors slip through untracked
