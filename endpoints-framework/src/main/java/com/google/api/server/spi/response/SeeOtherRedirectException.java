package com.google.api.server.spi.response;

/**
 * See other exception that is mapped to a 303 response and a redirection to the given location.
 */
public class SeeOtherRedirectException extends RedirectException {

  public static final int CODE = 303;

  public SeeOtherRedirectException(String statusMessage, String location) {
    super(CODE, statusMessage, location);
  }
}
