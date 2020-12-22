package com.google.api.server.spi.response;

import org.junit.Assert;
import org.junit.Test;

public class RedirectExceptionTest {

  @Test
  public void success() {
    RedirectException e = new RedirectException(302, "message", "location");
    Assert.assertEquals(302, e.getStatusCode());
    Assert.assertEquals("message", e.getMessage());
    Assert.assertEquals("location", e.getLocation());
  }

  @Test
  public void statusLimits_fails() {
    Assert.assertThrows(IllegalArgumentException.class, () -> new RedirectException(299, "message", "location"));
    Assert.assertThrows(IllegalArgumentException.class, () -> new RedirectException(400, "message", "location"));
  }

  @Test
  public void statusLimits_success() {
    RedirectException e = new RedirectException(300, "message", "location");
    Assert.assertEquals(300, e.getStatusCode());
    e = new RedirectException(399, "message", "location");
    Assert.assertEquals(399, e.getStatusCode());
  }

  @Test
  public void locationNull_fails() {
    Assert.assertThrows(NullPointerException.class, () -> new RedirectException(302, "message", null));
  }
}
