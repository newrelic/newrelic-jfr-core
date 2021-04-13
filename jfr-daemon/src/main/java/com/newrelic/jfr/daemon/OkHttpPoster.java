package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.http.HttpResponse;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import okhttp3.Authenticator;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpPoster implements HttpPoster {

  private final OkHttpClient okHttpClient;

  public OkHttpPoster(Proxy proxy, Authenticator proxyAuthenticator, Duration callTimeout) {
    final OkHttpClient.Builder builder = new OkHttpClient.Builder().callTimeout(callTimeout);

    if (proxy != null) {
      builder.proxy(proxy);
      if (proxyAuthenticator != null) {
        builder.authenticator(proxyAuthenticator);
      }
    }

    // FIXME required for HTTPS proxies? see https://github.com/square/okhttp/issues/6561
    //    builder.socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault()));

    this.okHttpClient = builder.build();
  }

  @Override
  public HttpResponse post(URL url, Map<String, String> headers, byte[] body, String mediaType)
      throws IOException {
    RequestBody requestBody = RequestBody.create(body, MediaType.get(mediaType));
    Request request =
        new Request.Builder().url(url).headers(Headers.of(headers)).post(requestBody).build();
    try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
      return new HttpResponse(
          response.body() != null ? response.body().string() : null,
          response.code(),
          response.message(),
          response.headers().toMultimap());
    }
  }
}
