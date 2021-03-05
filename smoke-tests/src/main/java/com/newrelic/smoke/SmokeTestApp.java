package com.newrelic.smoke;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@SpringBootApplication
public class SmokeTestApp {

  private static final Logger logger = LoggerFactory.getLogger(SmokeTestApp.class);

  public static void main(String[] args) {
    SpringApplication.run(SmokeTestApp.class, args);
  }

  @Bean
  public FilterRegistrationBean<GzipDecompressFilter> gzipDecompressFilter() {
    FilterRegistrationBean<GzipDecompressFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new GzipDecompressFilter());
    return registrationBean;
  }

  @RestController
  public static class Controller {

    private final AtomicLong metricCount = new AtomicLong();
    private final AtomicLong eventCount = new AtomicLong();

    @GetMapping("/ping")
    public String noTrace() {
      return "pong";
    }

    @PostMapping("/metric/add")
    public void addMetrics(@RequestBody List<Map<String, Object>> metrics) {
      metricCount.addAndGet(metrics.size());
      logger.info("Added {} metrics.", metrics.size());
    }

    @GetMapping("/metric/count")
    public long getMetricsCount() {
      long count = metricCount.get();
      logger.info("Current metric count is {}.", count);
      return count;
    }

    @PostMapping("/metric/clear")
    public void resetMetrics() {
      metricCount.set(0);
      logger.info("Resetting metric count.");
    }

    @PostMapping("/event/add")
    public void addEvents(@RequestBody List<Map<String, Object>> metrics) {
      eventCount.addAndGet(metrics.size());
      logger.info("Added {} events.", metrics.size());
    }

    @GetMapping("/event/count")
    public long getEventCount() {
      long count = eventCount.get();
      logger.info("Current event count is {}.", count);
      return count;
    }

    @PostMapping("/event/clear")
    public void resetEvents() {
      eventCount.set(0);
      logger.info("Resetting event count.");
    }
  }
}
