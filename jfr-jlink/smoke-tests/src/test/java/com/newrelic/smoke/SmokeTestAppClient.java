package com.newrelic.smoke;

import java.io.IOException;
import java.util.function.Function;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class SmokeTestAppClient {

  private final OkHttpClient httpClient;
  private final String host;
  private final int port;

  public SmokeTestAppClient(String host, int port) {
    this.httpClient = new OkHttpClient.Builder().build();
    this.host = host;
    this.port = port;
  }

  int getEventCount() {
    var request = new Request.Builder().url(url("/event/count")).build();
    return executeRequest(request, SmokeTestAppClient::parseForInteger);
  }

  int getMetricCount() {
    var request = new Request.Builder().url(url("/metric/count")).build();
    return executeRequest(request, SmokeTestAppClient::parseForInteger);
  }

  private String url(String path) {
    return String.format("http://%s:%s%s", host, port, path);
  }

  private static int parseForInteger(Response response) {
    try {
      var body = response.body();
      if (body == null) {
        throw new IllegalStateException("Cannot parse empty body to integer.");
      }
      return Integer.parseInt(body.string());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse response body to integer.", e);
    }
  }

  private <T> T executeRequest(Request request, Function<Response, T> responseHandler) {
    try (Response response = httpClient.newCall(request).execute()) {
      return responseHandler.apply(response);
    } catch (IOException e) {
      throw new IllegalStateException(
          String.format("Request to %s %s failed.", request.method(), request.url()), e);
    }
  }
}
