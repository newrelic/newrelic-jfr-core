package com.newrelic.jfr.daemon;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Manifest;

public class VersionFinder {

  public static final String UNKNOWN_VERSION = "UNKNOWN-VERSION";

  public String get() {
    var classLoader = VersionFinder.class.getClassLoader();
    if (!(classLoader instanceof URLClassLoader)) {
      return UNKNOWN_VERSION;
    }
    var cl = (URLClassLoader) classLoader;
    var url = cl.findResource("META-INF/MANIFEST.MF");
    if (url == null) {
      return UNKNOWN_VERSION;
    }
    var result = readManifest(url);
    return result == null ? UNKNOWN_VERSION : result;
  }

  private String readManifest(URL url) {
    try {
      var manifest = new Manifest(url.openStream());
      var attributes = manifest.getMainAttributes();
      return attributes.getValue("Implementation-Version");
    } catch (IOException e) {
      return UNKNOWN_VERSION;
    }
  }
}
