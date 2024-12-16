package com.google.api.server.spi.auth.microsoft.util;

public class UrlFormatter {
	
	public static final String TENANT_ID_PLACEHOLDER = "{tenantid}";
	
	/**
	 * Replaces the tenant id placeholder <i>{tenantId}</i> in the  url with the given tenant id.
	 * If there's no placeholder in the url, the url is returned as is.
	 */
	public static String withTenantId(String patternUrl, String tenantId) {
		return patternUrl.replace(TENANT_ID_PLACEHOLDER, tenantId);
	}

}
