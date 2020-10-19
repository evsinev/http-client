package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;

public class HttpClientImpl implements IHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientImpl.class);

    /**
     * Registers LocalThreadProxyAuthenticator
     *
     */
    public static void registerGlobalProxyAuthenticator() {
        System.getProperties().put("jdk.http.auth.tunneling.disabledSchemes", ""); // see https://bugs.openjdk.java.net/browse/JDK-8210814
        Authenticator.setDefault(new LocalThreadProxyAuthenticator());
    }

    @Override
    public HttpResponse send(HttpRequest aRequest, HttpRequestParameters aRequestParameters) throws HttpConnectException, HttpReadException, HttpWriteException {
        HttpProxyParameters proxyParameters = aRequestParameters.getProxyParameters();
        if(proxyParameters != null) {
            LocalThreadProxyAuthenticator.setParameters(proxyParameters);
        }
        try {
            String            url        = aRequest.getUrl();
            HttpURLConnection connection = createConnection(url, aRequest.getMethod(), aRequestParameters);

            sendHeaders(connection, aRequest.getHeaders());
            sendBody(url, connection, aRequest.getBody());

            return parseResponse(url, connection);
        } finally {
            if(proxyParameters != null) {
                LocalThreadProxyAuthenticator.clear();
            }
        }
    }

    private HttpResponse parseResponse(String aUrl, HttpURLConnection aConnection) throws HttpReadException {
        int              statusCode = waitForStatusCode(aUrl, aConnection);

        String reasonPhrase;
        try {
            reasonPhrase = aConnection.getResponseMessage();
        } catch (IOException e) {
            throw new HttpReadException("Cannot read reason phrase for url " + aUrl, e);
        }

        List<HttpHeader> headers = readHeaders(aConnection);
        byte[]           body    = readMessageBody(aUrl, statusCode, aConnection, headers);

        return new HttpResponse(statusCode, reasonPhrase, headers, body);
    }

    private int waitForStatusCode(String aUrl, HttpURLConnection aConnection) throws HttpReadException {
        LOG.debug("Waiting for response code for {} ...", aUrl);
        int statusCode;
        try {
            statusCode = aConnection.getResponseCode();
        } catch (IOException e) {
            throw new HttpReadException("Cannot wait for response code for url " + aUrl, e);
        }
        return statusCode;
    }

    private byte[] readMessageBody(String aUrl, int aStatusCode, HttpURLConnection aConnection, List<HttpHeader> aHeaders) throws HttpReadException {
        InputStream inputStream;
        try {
            inputStream = aStatusCode >= 400 ? aConnection.getErrorStream() : aConnection.getInputStream();
        } catch (IOException e) {
            throw new HttpReadException("Cannot create input stream for url " + aUrl, e);
        }

        int length = aConnection.getContentLength();
        if(length <= 0 ) {
            HttpHeaderFinder headerFinder = new HttpHeaderFinder(aHeaders);
            Optional<String> transferEncodingOption            = headerFinder.get("Transfer-Encoding");
            if(!transferEncodingOption.isPresent()) {
                return new byte[0];
            }
            if(transferEncodingOption.get().contains("chunked")) {
                try {
                    return readAllBytes(inputStream);
                } catch (IOException e) {
                    throw new HttpReadException("Cannot read chunked body from " + aUrl, e);
                }
            }

        }

        try {
            return readFully(inputStream, length);
        } catch (IOException e) {
            throw new HttpReadException("Cannot read message body from " + aUrl, e);
        }
    }


    private List<HttpHeader> readHeaders(HttpURLConnection aConnection) {
        Map<String, List<String>> headerFields = aConnection.getHeaderFields();
        List<HttpHeader> headers = new ArrayList<>(headerFields.size());

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if(entry == null) {
                continue;
            }

            String       name   = entry.getKey();
            if(name == null) {
                continue;
            }

            List<String> values = entry.getValue();
            if (values == null) {
                headers.add(new HttpHeader(name, ""));
                continue;
            }

            for (String value : values) {
                headers.add(new HttpHeader(name, value));
            }
        }

        return Collections.unmodifiableList(headers);
    }

    private void sendBody(String aUrl, HttpURLConnection aConnection, byte[] aRequestBody) throws HttpWriteException {
        if(aRequestBody == null || aRequestBody.length == 0) {
            return;
        }

        aConnection.setDoOutput(true);
        OutputStream outputStream = null;
        try {
            outputStream = aConnection.getOutputStream();
        } catch (IOException e) {
            throw new HttpWriteException("Cannot create output stream for url " + aUrl, e);
        }

        try {
            outputStream.write(aRequestBody);
        } catch (IOException e) {
            throw new HttpWriteException("Cannot create write body to url " + aUrl, e);
        }
    }

    private void sendHeaders(HttpURLConnection aConnection, HttpHeaders aHeaders) {
        if(aHeaders == null) {
            return;
        }

        for (HttpHeader header : aHeaders.asList()) {
            aConnection.setRequestProperty(header.getName(), header.getValue());
        }
    }

    private HttpURLConnection createConnection(String aUrl, HttpMethod aMethod, HttpRequestParameters aParameters) throws HttpConnectException {
        URL url;
        try {
            url = new URL(aUrl);
        } catch (MalformedURLException e) {
            throw new HttpConnectException("Cannot parse url: " + aUrl, e);
        }

        HttpURLConnection connection;
        try {
            connection = openConnection(aParameters, url);
        } catch (IOException e) {
            throw new HttpConnectException("Cannot open connection to " + aUrl, e);
        }

        connection.setConnectTimeout(aParameters.getTimeouts().getConnectTimeoutMs());
        connection.setReadTimeout(aParameters.getTimeouts().getReadTimeoutMs());

        try {
            connection.setRequestMethod(aMethod.name());
        } catch (ProtocolException e) {
            throw new HttpConnectException("Cannot set request method " + aMethod + " for url " + aUrl, e);
        }

        return connection;
    }

    private HttpURLConnection openConnection(HttpRequestParameters aParameters, URL url) throws IOException {
        HttpProxyParameters proxyParameters = aParameters.getProxyParameters();

        if(proxyParameters == null || proxyParameters.getProxy() == null) {
            return (HttpURLConnection) url.openConnection();
        }

        return (HttpURLConnection) url.openConnection(proxyParameters.getProxy());
    }

    public static byte[] readFully(InputStream aInputStream, int length) throws IOException {
        if(length <= 0) {
            return new byte[] {};
        }

        byte[] buffer = new byte[length];
        int    count  = 0;

        do {
            int read = aInputStream.read(buffer, count, length - count);
            if (read < 0) {
                throw new IOException("Read only " + count + " but wanted " + length + " (-1)");
            }
            count += read;
        } while (count < length);

        return buffer;
    }

    public static byte[] readAllBytes(InputStream aInputStream) throws IOException {
        List<byte[]> list   = new ArrayList<>();
        byte[]       buffer = new byte[2048];
        int          count;

        while( (count = aInputStream.read(buffer)) >= 0) {
            byte[] bytes = new byte[count];
            System.arraycopy(buffer, 0, bytes, 0, count);
            list.add(bytes);
        }

        int size = 0;
        for (byte[] bytes : list) {
            size += bytes.length;
        }

        byte[] output = new byte[size];
        int position = 0;
        for (byte[] bytes : list) {
            System.arraycopy(bytes, 0, output, position, bytes.length);
            position += bytes.length;
        }
        return output;
    }


}
