package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.http.HttpResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

public class ApachePoster implements HttpPoster {

  private CloseableHttpClient httpClient;

  public ApachePoster(DaemonConfig config) {
    if (config.getProxyHost() == null) {
      httpClient = HttpClients.createDefault();
    } else {
      int port = 80;
      if (config.getProxyPort() != null) {
        port = config.getProxyPort();
      } else {
        if ("https".equals(config.getProxyScheme())) {
          port = 443;
        }
      }
      HttpClientBuilder httpClientBuilder =
          HttpClients.custom().setProxy(new HttpHost(config.getProxyHost(), port));

      if (config.getProxyUser() != null) {
        AuthScope proxyScope = new AuthScope(config.getProxyHost(), port);
        UsernamePasswordCredentials credentials =
            new UsernamePasswordCredentials(
                config.getProxyUser(), config.getProxyPassword().toCharArray());
        BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(proxyScope, credentials);
        httpClientBuilder.setDefaultCredentialsProvider(credProvider);
      }

      httpClient = httpClientBuilder.build();
    }
  }

  @Override
  public HttpResponse post(URL url, Map<String, String> headers, byte[] body, String mediaType)
      throws IOException {
    try {
      ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.post(url.toURI());
      for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
        requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
      }
      requestBuilder.setEntity(new String(body)).setHeader("Content-Type", mediaType);

      ClassicHttpRequest request = requestBuilder.build();
      CloseableHttpResponse response = httpClient.execute(request);
      InputStream content = response.getEntity().getContent();
      String responseBody =
          new BufferedReader(new InputStreamReader(content))
              .lines()
              .collect(Collectors.joining("\n"));
      Map<String, List<String>> responseHeaders = new HashMap<>();
      for (Header header : response.getHeaders()) {
        List<String> values =
            responseHeaders.computeIfAbsent(header.getName(), k -> new ArrayList<>());
        values.add(header.getValue());
      }
      return new HttpResponse(
          responseBody, response.getCode(), response.getReasonPhrase(), responseHeaders);

    } catch (URISyntaxException e) {
      return null;
    }
  }
}
