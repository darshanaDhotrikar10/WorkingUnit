package com.smartbear.collaborator.client;

import java.net.URL;

/**
 * @author Nik
 */
public final class HttpClientConfig extends HttpClientConfigBase<HttpClientConfig> {

	/**
	 * Creates an empty HTTP client configuration.
	 */
	public HttpClientConfig() {
	}
	
	/**
	 * Creates an HTTP client configuration with server, user, proxy, and SSL settings.
	 *
	 * @param url server URL used by the HTTP client
	 * @param username user name used for authentication
	 * @param password password used for authentication
	 * @param proxyHost proxy host used for network requests
	 * @param proxyPort proxy port used for network requests
	 * @param isOverriteTrustStoreInSSL whether the SSL trust store should be overwritten
	 */
	public HttpClientConfig(URL url, String username, String password, String proxyHost, String proxyPort, Boolean isOverriteTrustStoreInSSL) {
		super(url, username, password, proxyHost, proxyPort, isOverriteTrustStoreInSSL);
	}
}
