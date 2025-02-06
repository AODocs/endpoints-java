package com.google.api.server.spi.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Clock;

public class GoogleCustomIdTokenVerifier extends GoogleIdTokenVerifier {
	
	public GoogleCustomIdTokenVerifier(HttpTransport transport, JsonFactory jsonFactory) {
		super(transport, jsonFactory);
	}
	protected GoogleCustomIdTokenVerifier(GoogleCustomIdTokenVerifier.Builder builder) {
		super(builder);
	}
	public GoogleCustomIdTokenVerifier(GooglePublicKeysManager publicKeys) {
		this(new GoogleCustomIdTokenVerifier.Builder(publicKeys));
	}
	
	
	
	public static class Builder extends GoogleIdTokenVerifier.Builder {
		
		public Builder(HttpTransport transport, JsonFactory jsonFactory) {
			super(transport, jsonFactory);
		}
		public Builder(GooglePublicKeysManager publicKeys) {
			super(publicKeys);
		}
		
		@Override
		public GoogleCustomIdTokenVerifier.Builder setIssuers(Collection<String> issuers) {
			return (Builder) super.setIssuers(issuers);
		}
		
		@Override
		public GoogleCustomIdTokenVerifier.Builder setAudience(Collection<String> audience) {
			return (Builder) super.setAudience(audience);
		}
		
		@Override
		public GoogleCustomIdTokenVerifier.Builder setClock(Clock clock) {
			return (Builder) super.setClock(clock);
		}
		
		@Override
		public GoogleCustomIdTokenVerifier.Builder setIssuer(String issuer) {
			return (Builder) super.setIssuer(issuer);
		}
		
		public GoogleCustomIdTokenVerifier build() {
			return new GoogleCustomIdTokenVerifier(this);
		}
		
	}
	
	@Override
	public GoogleCustomIdToken verify(String idTokenString) throws GeneralSecurityException, IOException {
		GoogleCustomIdToken idToken = GoogleCustomIdToken.parse(this.getJsonFactory(), idTokenString);
		return this.verify(idToken) ? idToken : null;
	}
	
}
