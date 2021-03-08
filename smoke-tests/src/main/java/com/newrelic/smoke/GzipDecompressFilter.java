package com.newrelic.smoke;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

public class GzipDecompressFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(SmokeTestApp.class);

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    boolean isGzipped =
        request.getHeader(HttpHeaders.CONTENT_ENCODING) != null
            && request.getHeader(HttpHeaders.CONTENT_ENCODING).contains("gzip");
    boolean requestTypeSupported = "POST".equals(request.getMethod());
    if (isGzipped && !requestTypeSupported) {
      throw new IllegalStateException(
          request.getMethod()
              + " is not supports gzipped body of parameters."
              + " Only POST requests are currently supported.");
    }
    String requestURI = request.getRequestURI();
    if (isGzipped) {
      logger.debug("Decompressing gzip POST request to {}", requestURI);
      InputStream inputStream = request.getInputStream();
      GZIPInputStream gis = new GZIPInputStream(inputStream);
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
      request = new GzipRequestWrapper(request, gis, bufferedReader);
    } else if (requestTypeSupported) {
      logger.debug("POST body to {} does not require gzip decompression.", requestURI);
    }
    chain.doFilter(request, response);
  }

  private static class GzipRequestWrapper extends HttpServletRequestWrapper {

    private final GzipServletInputStream servletInputStream;
    private final BufferedReader bufferedReader;

    public GzipRequestWrapper(
        ServletRequest request, GZIPInputStream inputStream, BufferedReader bufferedReader) {
      super((HttpServletRequest) request);
      this.servletInputStream = new GzipServletInputStream(inputStream);
      this.bufferedReader = bufferedReader;
    }

    @Override
    public ServletInputStream getInputStream() {
      return servletInputStream;
    }

    @Override
    public BufferedReader getReader() {
      return bufferedReader;
    }
  }

  private static class GzipServletInputStream extends ServletInputStream {

    private final GZIPInputStream gzipInputStream;

    private GzipServletInputStream(GZIPInputStream gzipInputStream) {
      this.gzipInputStream = gzipInputStream;
    }

    @Override
    public boolean isFinished() {
      try {
        return gzipInputStream.available() == 0;
      } catch (IOException e) {
        throw new IllegalStateException("Error occurred checking is stream is available.", e);
      }
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {}

    @Override
    public int read() throws IOException {
      return gzipInputStream.read();
    }
  }
}
