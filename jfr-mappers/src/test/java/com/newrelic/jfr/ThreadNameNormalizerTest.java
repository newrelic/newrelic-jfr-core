/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadNameNormalizerTest {
  private final ThreadNameNormalizer normalizer = getThreadNameNormalizer();

  public static ThreadNameNormalizer getThreadNameNormalizer() {
    return new ThreadNameNormalizer();
  }

  @Test
  public void hexInServerThreadName() {
    Assertions.assertEquals(
        "Server-#-#-selector-ServerConnectorManager@#",
        normalizer.getNormalizedThreadName(
            "Server-8091-130-selector-ServerConnectorManager@6182e1ea/7"));
  }

  @Test
  public void underlineDashWordbreaks() {
    Assertions.assertEquals(
        "ThreadInfo-timeslices-#-#_hello-one-two-#c.example.com-#-#_watcher_executor",
        normalizer.getNormalizedThreadName(
            "ThreadInfo/timeslices-84a3c-ae01b_hello-one-two-3c.example.com-3-74ae8_watcher_executor"));
  }

  @Test
  public void dontMatchShorterThan3Characters() {
    // won't match short hex numbers
    Assertions.assertEquals("caf", normalizer.getNormalizedThreadName("caf"));
    Assertions.assertEquals("#", normalizer.getNormalizedThreadName("cafe"));
    Assertions.assertEquals("caf-bab", normalizer.getNormalizedThreadName("caf-bab"));
    Assertions.assertEquals("caf-#", normalizer.getNormalizedThreadName("caf-babe"));
    Assertions.assertEquals("#", normalizer.getNormalizedThreadName("cafebabe"));
  }

  @Test
  public void removeSlashes() {
    // Make sure slashes are removed
    Assertions.assertEquals("foo@#", normalizer.getNormalizedThreadName("foo@bar/bat"));
  }

  @Test
  public void simpleNumbers() {
    Assertions.assertEquals("test-pool-#-#", normalizer.getNormalizedThreadName("test-pool-45-2"));
  }

  @Test
  public void objectHex() {
    Assertions.assertEquals(
        "net.sf.ehcache.CacheManager@#",
        normalizer.getNormalizedThreadName("net.sf.ehcache.CacheManager@b12bdc0"));
    // Assertions.assertEquals("org.eclipse.jetty.server.session.HashSessionManager@#Timer",
    // normalizer.getNormalizedThreadName("org.eclipse.jetty.server.session.HashSessionManager@2dbcae3Timer"));
  }

  @Test
  public void testWordBreaks() {

    // Recognize hex digits between word breaks but don't substitute word breaks
    Assertions.assertEquals(
        "timeslice-#-caf", normalizer.getNormalizedThreadName("timeslice-a87b34f0-caf"));
    Assertions.assertEquals(
        "timeslice-#-#", normalizer.getNormalizedThreadName("timeslice-a87b34f0-cafe"));
    Assertions.assertEquals(
        "timeslice#(#)caf", normalizer.getNormalizedThreadName("timeslice8(a87b34f0)caf"));
    Assertions.assertEquals(
        "timeslice#(#)caf", normalizer.getNormalizedThreadName("timeslice8(387b34f0)caf"));
    Assertions.assertEquals(
        "timeslices_prod-collector.example.com-#-#_watcher_executor",
        normalizer.getNormalizedThreadName(
            "timeslices_prod-collector.example.com-1404332009454-ba043e1b_watcher_executor"));
  }

  @Test
  public void httpVerbGet() {
    Assertions.assertEquals(
        "WebRequest#",
        normalizer.getNormalizedThreadName(
            "Threads/Time/CPU/dw-# - -flurry-zendesk-requester-email-alice@example.com? - GET -flurry-zendesk-requester-email-alice@example.com?/SystemTime"));
  }

  @Test
  public void httpVerbGetNegative() {
    Assertions.assertEquals("GettingTired", normalizer.getNormalizedThreadName("GettingTired"));
  }

  @Test
  public void httpVerbPut() {
    Assertions.assertEquals(
        "WebRequest#",
        normalizer.getNormalizedThreadName(
            "dw-# - PUT -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
  }

  @Test
  public void httpVerbPost() {
    Assertions.assertEquals(
        "WebRequest#",
        normalizer.getNormalizedThreadName(
            "dw-# - POST -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
  }

  @Test
  public void httpVerbDelete() {
    Assertions.assertEquals(
        "WebRequest#",
        normalizer.getNormalizedThreadName(
            "dw-# - DELETE -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
  }

  @Test
  public void httpVerbHead() {
    Assertions.assertEquals(
        "WebRequest#",
        normalizer.getNormalizedThreadName(
            "#.#.#.# [#] HEAD -content-careers-twitter-en.html HTTP-#.#"));
  }

  @Test
  public void sendingMailitem() {
    Assertions.assertEquals(
        "Sending mailitem#",
        normalizer.getNormalizedThreadName(
            "Sending mailitem To='dude@company.com' Subject='Updated: (EFEF-#) TCM Sep # onwards XSM ID #' From='null' FromName='Some Person (MY JIRA)' Cc='null' Bcc='null' ReplyTo='null' InReplyTo='null' MimeType='text-html' Encoding='UTF"));
  }

  @Test
  public void testSOAPProcessorThread() {
    Assertions.assertEquals(
        "SOAPProcessorThread#",
        normalizer.getNormalizedThreadName("SOAPProcessorThread1ecb5c3ba2fcae4a4e3d1"));
  }

  @Test
  public void testCookieBrokerUpdates() {
    Assertions.assertEquals(
        "CookieBrokerUpdates#",
        normalizer.getNormalizedThreadName("CookieBrokerUpdates-iwd4owfml6eth"));
  }

  @Test
  public void testC3P0PooledConnectionPoolManager() {
    Assertions.assertEquals(
        "C3P0PooledConnectionPoolManager#",
        normalizer.getNormalizedThreadName(
            "C3P0PooledConnectionPoolManager[identityToken->#bqq#hf#k#mickqhtf#b#r|#]-HelperThread-##"));
  }

  @Test
  public void testwildflyxnio() {
    Assertions.assertEquals(
        "xnio-file-watcher[#]-#",
        normalizer.getNormalizedThreadName(
            "xnio-file-watcher[Watcher for -usr-local-wildfly-standalone-tmp-vfs-temp-tempe#f#ad#-content-#-]-7364"));
  }

  @Test
  public void elasticsearch() {
    Assertions.assertEquals(
        "elasticsearch#",
        normalizer.getNormalizedThreadName(
            "elasticsearch[Speedball][transport_client_boss][T##]{New I-O boss ##}"));
  }

  @Test
  public void testOkHttp() {
    Assertions.assertEquals(
        "OkHttp https:#",
        normalizer.getNormalizedThreadName(
            "OkHttp https:--maps.googleapis.com-maps-api-geocode-json?client=gme-mycompany&latlng=#.#%#.#&signature=pkQTvo#Rqwz#yO#ceMvjT#qTY="));
  }

  @Test
  public void oldIO() {
    Assertions.assertEquals(
        "Old I-O client worker (#)",
        normalizer.getNormalizedThreadName(
            "Old I-O client worker ([id: #x#f#aa#f, -#.#.#.#:# => #-srv#.mycompany.net-#.#.#.#:#])"));
  }

  @Test
  public void akka() {
    Assertions.assertEquals(
        "default-akka.actor.default#",
        normalizer.getNormalizedThreadName(
            "default-akka.actor.default-dispatcher-# (Started: #, URL: https:--www.mycompany.com-abc-123-234-dfgd-asdf-dfasd, Phrase: asdfasd-234adfs-asdfas)"));
  }

  @Test
  public void testHashSessionManager() {
    Assertions.assertEquals(
        "org.eclipse.jetty.server.session.HashSessionManager#",
        normalizer.getNormalizedThreadName(
            "org.eclipse.jetty.server.session.HashSessionManager@2d3cfTimer"));
  }

  @Test
  public void testJobHandler() {
    Assertions.assertEquals(
        "JobHandler#",
        normalizer.getNormalizedThreadName(
            "JobHandler: -etc-workflow-instances-server#-#-#-#_#-request_for_activation_#:-content-mycompany-fr-ca-shared-header-header-slow-trucking"));
  }

  @Test
  public void testTransientResourceLock() {
    Assertions.assertEquals(
        "TransientResourceLock#",
        normalizer.getNormalizedThreadName("TransientResourceLock-KAUQ#DGps#AAAFYtAgf#LJ"));
  }

  @Test
  public void jmb() {
    Assertions.assertEquals(
        "jbm-client-session#",
        normalizer.getNormalizedThreadName(
            "jbm-client-session-l3a-2wv3dwi-7-wtuxwvvi-nwp4im-qq5o2c3"));
  }

  @Test
  public void uri() {
    Assertions.assertEquals(
        "http-thingie-#-exec-# uri:#",
        normalizer.getNormalizedThreadName(
            "http-thingie-#-exec-# uri:-secure-QuickEditIssue!default.jspa username:A#AC#ZZ"));
  }

  @Test
  public void capsHttp() {
    Assertions.assertEquals(
        "http-thingie-#-exec-# HTTP:#",
        normalizer.getNormalizedThreadName(
            "http-thingie-#-exec-# HTTP:-secure-QuickEditIssue!default.jspa username:A#AC#ZZ"));
  }

  @Test
  public void brackets() {
    Assertions.assertEquals(
        "rt-#-ResultCollector-[#]",
        normalizer.getNormalizedThreadName(
            "rt-#-ResultCollector-[[Hello World by GSS[#]] - MMMOAC#RF]"));
  }

  @Test
  public void multipleBrackets() {
    Assertions.assertEquals(
        "rt-#-ResultCollector-[#] value=[#] email=[#]",
        normalizer.getNormalizedThreadName(
            "rt-#-ResultCollector-[test] value=[dude] email=[p@thing.com]"));
  }
}
