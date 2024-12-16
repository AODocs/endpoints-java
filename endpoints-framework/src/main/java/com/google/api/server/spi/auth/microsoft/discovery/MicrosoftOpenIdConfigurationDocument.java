package com.google.api.server.spi.auth.microsoft.discovery;

import com.google.api.client.util.Key;

/**
 * Represents the OpenID discovery document for Microsoft.
 * Unused fields are omitted.
 */
public class MicrosoftOpenIdConfigurationDocument {
	
	@Key("issuer")
	private String issuer;
	
	@Key("userinfo_endpoint")
	private String userinfo_endpoint;
	
	@Key("jwks_uri")
	private String jwks_uri;
	
	public String getIssuer() {
		return issuer;
	}
	
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
	
	public String getUserinfo_endpoint() {
		return userinfo_endpoint;
	}
	
	public void setUserinfo_endpoint(String userinfo_endpoint) {
		this.userinfo_endpoint = userinfo_endpoint;
	}
	
	public String getJwks_uri() {
		return jwks_uri;
	}
	
	public void setJwks_uri(String jwks_uri) {
		this.jwks_uri = jwks_uri;
	}

}
