package com.google.api.server.spi.auth.microsoft;

import java.util.Arrays;

public enum SupportedVersion {
	
	V1_0("1.0"),
	V2_0("2.0");
	
	private final String version;
	
	SupportedVersion(String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	public static boolean isSupported(String version) {
		return Arrays.stream(SupportedVersion.values()).anyMatch(v -> v.version.equals(version));
	}
	
	public static SupportedVersion of(String version) {
		return Arrays.stream(SupportedVersion.values())
				.filter(v -> v.version.equals(version))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported Microsoft Oauth2 token version: " + version));
	}
}
