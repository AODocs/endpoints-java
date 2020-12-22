package com.google.api.server.spi.response;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_MULTIPLE_CHOICES;

import com.google.api.server.spi.ServiceException;
import com.google.common.base.Preconditions;

/**
 * Exception to be thrown by endpoint methods for returning a redirect response
 * instead of the declared response payload.
 * The status must be a 3xx redirect code.
 */
public class RedirectException extends ServiceException {

  private final String location;

  public RedirectException(int statusCode, String statusMessage, String location) {
    super(validateStatusCode(statusCode), statusMessage);
    this.location = Preconditions.checkNotNull(location);
  }

  public String getLocation() {
    return location;
  }

  private static int validateStatusCode(int statusCode) {
    if (statusCode < SC_MULTIPLE_CHOICES || statusCode >= SC_BAD_REQUEST) {
      throw new IllegalArgumentException("Not a 3xx redirect code: " + statusCode);
    }
    return statusCode;
  }
}
