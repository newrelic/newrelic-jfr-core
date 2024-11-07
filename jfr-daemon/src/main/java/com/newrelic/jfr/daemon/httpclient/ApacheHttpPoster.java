/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.httpclient;

import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.http.HttpResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache HTTP client used to POST JFR data. */
public class ApacheHttpPoster implements HttpPoster {
  private final ApacheProxyManager proxyManager;
  private final PoolingHttpClientConnectionManager connectionManager;
  private final CloseableHttpClient httpClient;
  private static final Logger logger = LoggerFactory.getLogger(ApacheHttpPoster.class);

  public ApacheHttpPoster(
      ApacheProxyManager proxyManager, SSLContext sslContext, int defaultTimeoutInMillis) {
    this.proxyManager = proxyManager;
    // Currently, sslContext is always set to null when the HttpPoster is constructed
    // in SetupUtils#buildTelemetryClient, which means that an SSL context will
    // be created by SSLConnectionSocketFactory.getSocketFactory(). However, we could
    // create a custom SSL context based on a config like ca_bundle_path if desired.
    this.connectionManager = createHttpClientConnectionManager(sslContext);
    this.httpClient = createHttpClient(defaultTimeoutInMillis);
  }

  private static PoolingHttpClientConnectionManager createHttpClientConnectionManager(
      SSLContext sslContext) {
    // Using the pooling manager here for thread safety.
    PoolingHttpClientConnectionManager httpClientConnectionManager =
        new PoolingHttpClientConnectionManager(
            RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register(
                    "https",
                    sslContext != null
                        ? new SSLConnectionSocketFactory(sslContext)
                        : SSLConnectionSocketFactory.getSocketFactory())
                .build());

    httpClientConnectionManager.setMaxTotal(1);
    httpClientConnectionManager.setDefaultMaxPerRoute(1);

    return httpClientConnectionManager;
  }

  private CloseableHttpClient createHttpClient(int requestTimeoutInMillis) {
    HttpClientBuilder builder =
        HttpClientBuilder.create()
            .setDefaultHeaders(
                Arrays.<Header>asList(
                    new BasicHeader("Connection", "Keep-Alive"),
                    new BasicHeader("CONTENT-TYPE", "application/json")))
            .setSSLHostnameVerifier(new DefaultHostnameVerifier())
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    // Timeout in millis until a connection is established.
                    .setConnectTimeout(requestTimeoutInMillis)
                    // Timeout in millis when requesting a connection from the connection manager.
                    // This timeout should be longer than the connect timeout to avoid potential
                    // ConnectionPoolTimeoutExceptions.
                    .setConnectionRequestTimeout(requestTimeoutInMillis * 2)
                    // Timeout in millis for non-blocking socket I/O operations (aka max inactivity
                    // between two consecutive data packets).
                    .setSocketTimeout(requestTimeoutInMillis)
                    .build())
            .setDefaultSocketConfig(
                SocketConfig.custom()
                    // Timeout in millis for non-blocking socket I/O operations.
                    .setSoTimeout(requestTimeoutInMillis)
                    .setSoKeepAlive(true)
                    .build())
            .setConnectionManager(connectionManager);

    if (proxyManager.getProxy() != null) {
      builder.setProxy(proxyManager.getProxy());
    }

    return builder.build();
  }

  public void shutdown() {
    connectionManager.closeIdleConnections(0, TimeUnit.SECONDS);
  }

  private HttpContext createContext() {
    return proxyManager.updateContext(HttpClientContext.create());
  }

  @Override
  public HttpResponse post(URL url, Map<String, String> headers, byte[] body, String mediaType)
      throws IOException {
    try {
      RequestBuilder requestBuilder = RequestBuilder.post();

      requestBuilder
          .setUri(url.toURI())
          .setEntity(new ByteArrayEntity(body))
          .setHeader("Content-Type", mediaType);

      for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
        requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
      }

      HttpUriRequest apacheRequest = requestBuilder.build();

      try (CloseableHttpResponse response = httpClient.execute(apacheRequest, createContext())) {
        InputStream content = response.getEntity().getContent();

        String responseBody =
            new BufferedReader(new InputStreamReader(content))
                .lines()
                .collect(Collectors.joining("\n"));

        Map<String, List<String>> responseHeaders = new HashMap<>();

        for (Header header : response.getAllHeaders()) {
          List<String> values =
              responseHeaders.computeIfAbsent(header.getName(), k -> new ArrayList<>());
          values.add(header.getValue());
        }
        return new HttpResponse(
            responseBody,
            response.getStatusLine().getStatusCode(),
            response.getStatusLine().getReasonPhrase(),
            responseHeaders);
      }
    } catch (URISyntaxException e) {
      logger.info("JFR HttpPoster: Exception posting data.", e);
      return null;
    }
  }
}
