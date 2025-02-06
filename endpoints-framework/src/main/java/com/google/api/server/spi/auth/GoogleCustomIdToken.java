package com.google.api.server.spi.auth;

import java.io.IOException;

import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Key;

public class GoogleCustomIdToken extends GoogleIdToken {
	
	public GoogleCustomIdToken(Header header, Payload payload, byte[] signatureBytes, byte[] signedContentBytes) {
		super(header, payload, signatureBytes, signedContentBytes);
	}
	
	public static GoogleCustomIdToken parse(JsonFactory jsonFactory, String idTokenString) throws IOException {
		JsonWebSignature jws = JsonWebSignature.parser(jsonFactory).setPayloadClass(Payload.class).parse(idTokenString);
		return new GoogleCustomIdToken(jws.getHeader(), (Payload) jws.getPayload(), jws.getSignatureBytes(), jws.getSignedContentBytes());
	}
	
	public GoogleCustomIdToken.Payload getPayload() {
		return (GoogleCustomIdToken.Payload)super.getPayload();
	}
	
	public static class Payload extends GoogleIdToken.Payload {
		
		@Key("primaryEmail")
		private String primaryEmail;
		
		public String getPrimaryEmail() {
			return primaryEmail;
		}
		
		public void setPrimaryEmail(String primaryEmail) {
			this.primaryEmail = primaryEmail;
		}
		
	}
}
