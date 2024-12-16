package com.google.api.server.spi.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.http.HttpServletRequest;

import com.google.api.server.spi.Client;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.auth.microsoft.MicrosoftIdToken;
import com.google.api.server.spi.auth.microsoft.MicrosoftTokenVerifier;
import com.google.api.server.spi.auth.microsoft.discovery.MicrosoftOpenIDConfigurationDocumentProvider;
import com.google.api.server.spi.auth.microsoft.jwks.PublicKeyProvider;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.request.Attribute;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;

/**
 * Authenticator for Microsoft issued JSON Web Token, currently specific for Microsoft Id Tokens (v1.0 and v2.0).
 */
public class MicrosoftOAuth2Authenticator implements Authenticator {
	
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	
	private final MicrosoftTokenVerifier verifier;
	
	public MicrosoftOAuth2Authenticator() {
		this(new MicrosoftTokenVerifier(
				Client.getInstance().getJsonFactory(),
				new MicrosoftOpenIDConfigurationDocumentProvider(),
				new PublicKeyProvider()));
	}
	
	public MicrosoftOAuth2Authenticator(MicrosoftTokenVerifier verifier) {
		this.verifier = verifier;
	}
	
	@VisibleForTesting
	MicrosoftIdToken verifyToken(String token) {
		if (token == null) {
			return null;
		}
		try {
			return verifier.verify(token);
		} catch (GeneralSecurityException | IOException | IllegalArgumentException | ServiceException e) {
			logger.atWarning().withCause(e).log("error while verifying JWT");
			return null;
		}
	}
	
	@Override
	public User authenticate(HttpServletRequest request) {
		Attribute attr = Attribute.from(request);
		if (attr.isEnabled(Attribute.SKIP_TOKEN_AUTH)) {
			return null;
		}
		
		String token = GoogleAuth.getAuthToken(request);
		if (!GoogleAuth.isJwt(token)) {
			return null;
		}
		
		MicrosoftIdToken microsoftToken = verifyToken(token);
		if (microsoftToken == null) {
			return null;
		}
		
		attr.set(Attribute.MICROSOFT_ID_TOKEN, microsoftToken);
		
		String userId = microsoftToken.getPayload().getObjectId();
		String email = microsoftToken.getPayload().getEmail();
		
		User user = (userId == null && email == null) ? null : new User(userId, email);
		if (attr.isEnabled(Attribute.REQUIRE_APPENGINE_USER)) {
			com.google.appengine.api.users.User appEngineUser =
					(email == null) ? null : new com.google.appengine.api.users.User(email, "");
			logger.atInfo().log("appEngineUser = %s", appEngineUser);
			attr.set(Attribute.AUTHENTICATED_APPENGINE_USER, appEngineUser);
		} else {
			logger.atInfo().log("user = %s", user);
		}
		return user;
	}
	
}