/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionFinder {

  private static final String UNKNOWN_VERSION = "UNKNOWN-VERSION";

  private VersionFinder() {}

  /**
   * Retrieve the version of the daemon running.
   *
   * @return the version string
   */
  public static String getVersion() {
    ClassLoader classLoader = VersionFinder.class.getClassLoader();
    if (!(classLoader instanceof URLClassLoader)) {
      return UNKNOWN_VERSION;
    }
    URLClassLoader cl = (URLClassLoader) classLoader;
    URL url = cl.findResource("META-INF/MANIFEST.MF");
    if (url == null) {
      return UNKNOWN_VERSION;
    }
    String result = readManifest(url);
    return result == null ? UNKNOWN_VERSION : result;
  }

  private static String readManifest(URL url) {
    try {
      Manifest manifest = new Manifest(url.openStream());
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Implementation-Version");
    } catch (IOException e) {
      return UNKNOWN_VERSION;
    }
  }
}
