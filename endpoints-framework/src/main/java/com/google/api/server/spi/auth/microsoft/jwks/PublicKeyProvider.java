package com.google.api.server.spi.auth.microsoft.jwks;

import java.net.URL;
import java.security.PublicKey;
import java.util.logging.Logger;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

public class PublicKeyProvider {
	
	private static final Logger sLogger = Logger.getLogger(PublicKeyProvider.class.getName());
	
	public PublicKey getPublicKey(String jwksUri, JsonWebSignature.Header header) throws ServiceUnavailableException {
		try {
			URL jwkSetURL = new URL(jwksUri);
			
			JWKSet jwkSet = JWKSet.load(jwkSetURL);
			
			JWSAlgorithm algorithm = JWSAlgorithm.parse(header.getAlgorithm());
			if (algorithm.equals(JWSAlgorithm.RS256)) {
				RSAKey rsaKey = null;
				for (JWK jwk : jwkSet.getKeys()) {
					if (jwk.getKeyID().equals(header.getKeyId())) {
						rsaKey = (RSAKey) jwk;
						break;
					}
				}
				if (rsaKey == null) {
					throw new IllegalArgumentException("Cannot locate publicKey for algorithm: " + header.getAlgorithm());
				}
				
				sLogger.info("Public key: " + rsaKey.toRSAPublicKey());
				
				return rsaKey.toRSAPublicKey();
			} else {
				throw new IllegalArgumentException("Unsupported algorithm: " + header.getAlgorithm());
			}
		} catch (Exception e) {
			throw new ServiceUnavailableException("Failed to obtain public key for signature validation", e);
		}
	}
}
