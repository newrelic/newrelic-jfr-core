/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.httpclient;

import com.newrelic.jfr.daemon.DaemonConfig;
import java.text.MessageFormat;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages setup for a proxy used by the ApacheHttpPoster */
public class ApacheProxyManager {
  private final HttpHost proxy;
  private final Credentials proxyCredentials;
  private static final Logger logger = LoggerFactory.getLogger(ApacheProxyManager.class);

  public ApacheProxyManager(DaemonConfig config) {
    String proxyHost = config.getProxyHost();
    Integer proxyPort = config.getProxyPort();
    String proxyScheme = config.getProxyScheme();
    String proxyUser = config.getProxyUser();
    String proxyPassword = config.getProxyPassword();

    if (proxyHost != null && proxyPort != null && proxyScheme != null) {
      proxy = new HttpHost(proxyHost, proxyPort, proxyScheme);
      proxyCredentials = getProxyCredentials(proxyUser, proxyPassword);

      logger.info(
          "JFR HttpPoster: configured to use "
              + proxyScheme
              + " proxy: "
              + proxyHost
              + ":"
              + proxyPort);
    } else {
      proxy = null;
      proxyCredentials = null;
    }
  }

  private Credentials getProxyCredentials(final String proxyUser, final String proxyPass) {
    if (proxyUser != null && proxyPass != null) {
      logger.info(
          MessageFormat.format(
              "JFR HttpPoster: Setting Proxy Authenticator for user {0}", proxyUser));
      return new UsernamePasswordCredentials(proxyUser, proxyPass);
    }
    return null;
  }

  public HttpHost getProxy() {
    return proxy;
  }

  public HttpContext updateContext(HttpClientContext httpClientContext) {
    if (proxy != null && proxyCredentials != null) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(new AuthScope(proxy), proxyCredentials);
      httpClientContext.setCredentialsProvider(credentialsProvider);
    }

    return httpClientContext;
  }
}
