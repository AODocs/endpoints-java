package com.google.api.server.spi.response;

/**
 * Found exception that is mapped to a 302 response and a redirection to the given location.
 */
public class FoundRedirectException extends RedirectException {

  public static final int CODE = 302;

  public FoundRedirectException(String statusMessage, String location) {
    super(CODE, statusMessage, location);
  }
}
