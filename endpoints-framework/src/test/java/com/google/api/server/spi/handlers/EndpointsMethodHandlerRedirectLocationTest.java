package com.google.api.server.spi.handlers;

import static com.google.api.server.spi.handlers.EndpointsMethodHandler.getRedirectLocation;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.api.server.spi.EndpointsContext;

/**
 * Tests for redirect location in {@link EndpointsMethodHandler}.
 */
@RunWith(JUnit4.class)
public class EndpointsMethodHandlerRedirectLocationTest {

  private EndpointsContext context;
  private MockHttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    request = new MockHttpServletRequest();
    request.setRequestURI("/_ah/api/resource");
    HttpServletResponse response = new MockHttpServletResponse();
    context = new EndpointsContext("", "", request, response, false);
  }

  @Test
  public void httpsUrl() {
    Assert.assertEquals("https://example.com/resource", getRedirectLocation(context, "https://example.com/resource"));
  }

  @Test
  public void httpsUrlWithPort() {
    Assert.assertEquals("https://example.com:8443/resource", getRedirectLocation(context, "https://example.com:8443/resource"));
  }

  @Test
  public void relativeToServer() {
    Assert.assertEquals("/redirected/other", getRedirectLocation(context, "/redirected/other"));
  }

  @Test
  public void relativeToRequest() {
    Assert.assertEquals("/_ah/api/resource/redirect", getRedirectLocation(context, "redirect"));
  }

  @Test
  public void relativeToRequest_trailingSlashInRequest() {
    request.setRequestURI("/_ah/api/resource/");
    Assert.assertEquals("/_ah/api/resource/redirect", getRedirectLocation(context, "redirect"));
  }
}
