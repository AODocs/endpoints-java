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
  public void statusOk_fails() {
    Assert.assertThrows(IllegalArgumentException.class, () -> new RedirectException(200, "message", "location"));
  }

  @Test
  public void statusBadRequest_fails() {
    Assert.assertThrows(IllegalArgumentException.class, () -> new RedirectException(400, "message", "location"));
  }

  @Test
  public void locationNull_fails() {
    Assert.assertThrows(NullPointerException.class, () -> new RedirectException(302, "message", null));
  }
}
