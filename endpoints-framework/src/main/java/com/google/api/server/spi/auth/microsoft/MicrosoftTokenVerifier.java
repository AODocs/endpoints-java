package com.google.api.server.spi.auth.microsoft;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import com.google.api.client.json.JsonFactory;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.microsoft.discovery.MicrosoftOpenIdConfigurationDocument;
import com.google.api.server.spi.auth.microsoft.discovery.MicrosoftOpenIDConfigurationDocumentProvider;
import com.google.api.server.spi.auth.microsoft.jwks.PublicKeyProvider;
import com.google.api.server.spi.auth.microsoft.util.UrlFormatter;
import com.google.api.server.spi.response.ServiceUnavailableException;

public class MicrosoftTokenVerifier {
	
	public static final long DEFAULT_TIME_SKEW_SECONDS = 300;
	
	private final JsonFactory jsonFactory;
	private final MicrosoftOpenIDConfigurationDocumentProvider configurationProvider;
	private final PublicKeyProvider publicKeyProvider;
	
	public MicrosoftTokenVerifier(JsonFactory jsonFactory, MicrosoftOpenIDConfigurationDocumentProvider configurationProvider, PublicKeyProvider publicKeyProvider) {
		this.jsonFactory = jsonFactory;
		this.configurationProvider = configurationProvider;
		this.publicKeyProvider = publicKeyProvider;
	}
	
	public MicrosoftIdToken verify(String token) throws GeneralSecurityException, IOException, ServiceException {
		MicrosoftIdToken idToken = MicrosoftIdToken.parse(jsonFactory, token);
		return verify(idToken) ? idToken : null;
	}
	
	private boolean verify(MicrosoftIdToken idToken) throws ServiceException, GeneralSecurityException {
		String version = idToken.getPayload().getVersion();
		String tenantId = idToken.getPayload().getTenantId();
		MicrosoftOpenIdConfigurationDocument discoveryDocument = configurationProvider.getConfigurationDocument(SupportedVersion.of(version), tenantId);
		
		return idToken.verifyVersion() &&
				idToken.verifyIssuer(getExpectedIssuer(discoveryDocument, tenantId)) &&
				idToken.verifyTime(System.currentTimeMillis(), DEFAULT_TIME_SKEW_SECONDS) &&
				idToken.verifySignature(getPublicKey(discoveryDocument, idToken));
	}
	
	private PublicKey getPublicKey(MicrosoftOpenIdConfigurationDocument discoveryDocument, MicrosoftIdToken idToken) throws ServiceUnavailableException {
		return publicKeyProvider.getPublicKey(discoveryDocument.getJwks_uri(), idToken.getHeader());
	}
	
	private String getExpectedIssuer(MicrosoftOpenIdConfigurationDocument discoveryDocument, String tenantId) {
		return UrlFormatter.withTenantId(discoveryDocument.getIssuer(), tenantId);
	}
	
}
