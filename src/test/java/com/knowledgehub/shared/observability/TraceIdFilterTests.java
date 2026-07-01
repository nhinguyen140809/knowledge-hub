package com.knowledgehub.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTests {

  private final TraceIdFilter filter = new TraceIdFilter();

  @Test
  void generatesATraceIdExposesItAndClearsItAfterTheRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] seenDuringRequest = new String[1];
    FilterChain chain = (req, res) -> seenDuringRequest[0] = MDC.get(TraceIdFilter.TRACE_ID);

    filter.doFilter(request, response, chain);

    assertThat(seenDuringRequest[0]).isNotBlank();
    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo(seenDuringRequest[0]);
    // The id must not leak into the next request handled on this thread.
    assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
  }

  @Test
  void honoursAnInboundTraceIdForCrossBoundaryCorrelation() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "agent-trace-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] seenDuringRequest = new String[1];
    FilterChain chain = (req, res) -> seenDuringRequest[0] = MDC.get(TraceIdFilter.TRACE_ID);

    filter.doFilter(request, response, chain);

    assertThat(seenDuringRequest[0]).isEqualTo("agent-trace-123");
    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("agent-trace-123");
  }

  @Test
  void rejectsAnUnsafeInboundTraceIdToPreventLogForging() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "abc\nINFO forged-log-line");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] seenDuringRequest = new String[1];
    FilterChain chain = (req, res) -> seenDuringRequest[0] = MDC.get(TraceIdFilter.TRACE_ID);

    filter.doFilter(request, response, chain);

    // The malicious value is dropped and a safe, generated id is used instead.
    assertThat(seenDuringRequest[0]).doesNotContain("\n").doesNotContain(" ");
    assertThat(seenDuringRequest[0]).isNotEqualTo("abc\nINFO forged-log-line");
  }

  @Test
  void clearsTheTraceIdEvenWhenTheChainFails() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain failing =
        (req, res) -> {
          throw new RuntimeException("boom");
        };

    try {
      filter.doFilter(request, response, failing);
    } catch (Exception ignored) {
      // expected
    }

    assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
  }
}
