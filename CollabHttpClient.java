package com.smartbear.collaborator.client;


import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.Header;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.http.Consts.UTF_8;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

/**
 *  This class handles httpclient request and response after 11.5.
 *  Upgrade httpclient to 4.5.6.
 */
public class CollabHttpClient {

    private Map<String, String> headers = new HashMap<>();
    public static final byte[] EMPTY_BYTES = new byte[0];

    private boolean preemptive;
    private HttpClientBuilder httpClientBuilder;
    private URIBuilder uriBuilder;
    private RequestConfig config;
    private HttpClientContext context;
    private HttpRequestBase httpRequestBase;
    private HttpClient client;
    private CredentialsProvider credsProvider;

    /**
     * Creates an HTTP client builder and sets the user agent for outgoing requests.
     *
     * @param userAgent user agent value sent with HTTP requests
     */
    public CollabHttpClient(String userAgent){
        httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setUserAgent(userAgent);
    }


    /**
     * Creates an HTTP client using the supplied client configuration and optional SSL settings.
     *
     * @param config client configuration that contains URL and user agent details
     * @param sslConnectionSocketFactory optional SSL socket factory for HTTPS connections
     * @throws URISyntaxException if the configured URL is not valid
     */
    public CollabHttpClient(HttpClientConfigBase<?> config, SSLConnectionSocketFactory sslConnectionSocketFactory ) throws   URISyntaxException{
        this(config.getUserAgent());

        if(sslConnectionSocketFactory != null) {
            httpClientBuilder.setSSLSocketFactory(sslConnectionSocketFactory);
        }

        String url = config.getUrl().toString();
        setUrl(url);
    }


    /**
     * Enables or disables preemptive authentication.
     *
     * @param preemptive true to send authentication details before the server challenges the request
     */
    public void setPreemptive(boolean preemptive){
        this.preemptive = preemptive;
    }

    /**
     * Sets the base URL used to build request URIs.
     *
     * @param url base URL for the target server
     * @throws URISyntaxException if the URL is not valid
     */
    public void setUrl(String url) throws URISyntaxException {
        uriBuilder = new URIBuilder(url);

    }

    /**
     * Sets the retry handler used by the HTTP client.
     *
     * @param httpRequestRetryHandler retry handler for failed HTTP requests
     */
    public void setHttpRequestRetryHandler(HttpRequestRetryHandler httpRequestRetryHandler){
        if (httpRequestRetryHandler != null) {
            httpClientBuilder.setRetryHandler(httpRequestRetryHandler);
        }
    }

    /**
     * Sets connection, request, and socket timeouts for the HTTP client.
     *
     * @param timeout timeout value in milliseconds
     */
    public void setTimeout(Integer timeout){
        //Timeout
        if(timeout != null) {
            int timeoutValue = timeout.intValue();

            config = RequestConfig.custom()
                    .setConnectTimeout(timeoutValue)
                    .setConnectionRequestTimeout(timeoutValue)
                    .setSocketTimeout(timeoutValue).build();
            httpClientBuilder.setDefaultRequestConfig(config);
        }
    }

    /**
     * Sets the proxy server and optional proxy credentials for outgoing requests.
     *
     * @param proxyHost proxy host name
     * @param proxyPort proxy port number
     * @param username proxy user name, if authentication is required
     * @param password proxy password, if authentication is required
     */
    public void setProxy(String proxyHost, int proxyPort, String username, String password){
        //Proxy
        if(proxyHost != null){
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpClientBuilder.setProxy(proxy);
            if(username != null && password != null)
                credsProvider.setCredentials(
                        new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * Adds basic authentication credentials to the HTTP client context.
     *
     * @param username user name used for server authentication
     * @param password password used for server authentication
     * @param uri target server URI
     * @param httpRequest current request, used to avoid replacing an existing authorization header
     */
    public void authenticate(String username, String password, URI uri, HttpRequestBase httpRequest){
        if (httpRequest != null && httpRequest.getFirstHeader(AUTHORIZATION) != null) {
            return;
        }
        Credentials credentials = new UsernamePasswordCredentials(username,password);
        AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort(),  AuthScope.ANY_REALM);
        credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(authScope, credentials);

        context = HttpClientContext.create();
        context.setCredentialsProvider(credsProvider);

        if(preemptive){
            HttpHost targetHost = new HttpHost( uri.getHost(),  uri.getPort(), uri.getScheme());
            AuthCache authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());
            context.setAuthCache(authCache);
        }

    }

    /**
     * Sets the request object, applies the request path, and builds the final request URI.
     *
     * @param httpRequestBase request object that will be executed
     * @param path optional path to apply to the base URL
     * @throws URISyntaxException if the final request URI is not valid
     */
    public void setHttpRequestBase(HttpRequestBase httpRequestBase, String path) throws URISyntaxException{
        this.httpRequestBase = httpRequestBase;
        if(path != null) {
            uriBuilder.setPath(path);
        }
        URI uri = uriBuilder.build();
        this.httpRequestBase.setURI(uri);
        this.httpRequestBase.setProtocolVersion(HttpVersion.HTTP_1_1);

    }

    /**
     * Adds a query parameter to the request URI.
     *
     * @param name query parameter name
     * @param value query parameter value
     */
    public void addParameter(String name, String value){
        uriBuilder.addParameter(name, value);
    }

    /**
     * Replaces all request headers with the supplied header map.
     *
     * @param headers headers to send with the request
     */
    public void setHeaders(Map<String, String> headers){
        this.headers = headers;

    }

    /**
     * Adds or updates one request header.
     *
     * @param name header name
     * @param value header value
     */
    public void setHeader(String name, String value){
        this.headers.put(name,value);
    }

    /**
     * Builds the HTTP client from the configured builder settings.
     */
    public void buildHttpClient(){
        client  =  httpClientBuilder.build();
    }

    /**
     * Sets the request object and executes it.
     *
     * @param httpRequestBase request object to execute
     * @return response status and response data
     * @throws IOException if the request fails
     * @throws URISyntaxException if the request URI is not valid
     */
    public ResponseInfo execute(HttpRequestBase httpRequestBase) throws IOException, URISyntaxException {
        setHttpRequestBase(httpRequestBase, null);
        return execute();
    }

    /**
     * Executes the configured HTTP request and returns response information.
     *
     * @return response status and response data
     * @throws IOException if the request fails
     */
    public ResponseInfo execute() throws IOException {
        try {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpRequestBase.setHeader(entry.getKey(), entry.getValue());
            }

            HttpResponse response = client.execute(httpRequestBase, context);
            int status = response.getStatusLine().getStatusCode();

            return new ResponseInfo(status, response);
        }catch(Throwable e){
            e.printStackTrace();
            throw new IOException(e.getMessage(), e);
        }
    }

    public static class ResponseInfo{
        int status;
        
        HttpResponse response;


        /**
         * Stores the HTTP status code and raw HTTP response.
         *
         * @param status HTTP status code returned by the server
         * @param response raw HTTP response returned by the server
         */
        public ResponseInfo(int status, HttpResponse response){
            this.status = status;
            this.response = response;
        }

        /**
         * Returns the HTTP status code.
         *
         * @return HTTP status code
         */
        public int getStatus(){
            return this.status;
        }

        /**
         * Returns the response body as text using UTF-8.
         *
         * @return response body text
         */
        public String getResponseBody() {
            byte[] responseBytes = getResponseBytes();
            return new String(responseBytes, UTF_8);
        }

        /**
         * Returns the response body as bytes.
         *
         * @return response body bytes, or empty bytes when no body can be read
         */
        public byte[] getResponseBytes() {
            byte[] responseBytes = EMPTY_BYTES;
            try {
                HttpEntity entity = response.getEntity();  //  response.getEntity().getContent()??

                if (status != HttpStatus.SC_NO_CONTENT) {
                    responseBytes = EntityUtils.toByteArray(entity);
                }
            } catch (IOException e) {
                return EMPTY_BYTES;
            }
            return responseBytes;
        }

        /**
         * Returns the raw HTTP response object.
         *
         * @return raw HTTP response
         */
        public HttpResponse getResponse(){
            return response;
        }

        /**
         * Returns all response headers.
         *
         * @return all response headers, or null when no response is available
         */
        public Header[] getAllHeaders(){
            return response == null?  null : response.getAllHeaders();
        }

        /**
         * Returns all response headers that match the given name.
         *
         * @param name header name to find
         * @return matching response headers, or null when no response is available
         */
        public Header[] getHeaders(String name){
            return response == null?  null :  response.getHeaders(name);
        }

        /**
         * Returns the first response header that matches the given name.
         *
         * @param name header name to find
         * @return first matching response header, or null when no response is available
         */
        public  Header	getFirstHeader(String name){
            return response == null?  null :  response.getFirstHeader(  name);
        }

    }
}
