package com.payneteasy.http.client.okhttp;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpProxyAuthConnectionException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;
import kotlin.Pair;
import lombok.NonNull;
import okhttp3.*;
import okhttp3.internal.Util;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class HttpClientOkHttpImpl implements IHttpClient {

    private final OkHttpClient defaultClient;


    public HttpClientOkHttpImpl() {
        this(new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .build()
        );
    }

    public HttpClientOkHttpImpl(OkHttpClient aDefaultClient) {
        defaultClient = aDefaultClient;
    }

    @Override
    public HttpResponse send(HttpRequest aRequest, HttpRequestParameters aRequestParameters) throws HttpConnectException, HttpReadException, HttpWriteException {
        Request      request  = createRequest(aRequest);
        OkHttpClient client   = createClient(aRequestParameters);
        Call         call     = client.newCall(request);
        String       url      = aRequest.getUrl();
        Response     response = executeCall(call, url);
        return createResponse(response);
    }

    @NotNull
    private Response executeCall(Call call, @NonNull String url) throws HttpConnectException, HttpReadException {
        long         starTimeMs = System.currentTimeMillis();
        Response response;
        try {
            response = call.execute();
        } catch (SSLHandshakeException |SSLPeerUnverifiedException e) {
            throw new HttpConnectException("Bad ssl certificate at " + url, e);
        } catch (ProtocolException e) {
            if("Unexpected status line: <html><head><title>407 Proxy Authentication Required</title></head>".equals(e.getMessage())) {
                throw new HttpConnectException("407 Proxy Authentication Required", e);
            } else {
                throw new HttpConnectException("Protocol error: cannot connect to " + url + " within " + (System.currentTimeMillis() - starTimeMs) + " ms", e);
            }
        } catch (ConnectException e) {
            throw new HttpConnectException("Cannot connect to " + url + " within " + (System.currentTimeMillis() - starTimeMs) + " ms", e);
        } catch (SocketTimeoutException e) {
            throw new HttpConnectException("Connection timed out to " + url + " within " + (System.currentTimeMillis() - starTimeMs) + " ms", e);
        } catch (InterruptedIOException e) {
            throw new HttpConnectException("Connection interrupted to " + url + " within " + (System.currentTimeMillis() - starTimeMs) + " ms", e);
        } catch (IOException e) {
            if("Failed to authenticate with proxy".equals(e.getMessage())) {
                throw new HttpProxyAuthConnectionException("Failed to authenticate with proxy", e);
            }
            
            throw new HttpReadException("Cannot read from " + url, e);
        }
        return response;
    }

    @NotNull
    private HttpResponse createResponse(Response aResponse) throws HttpReadException {
        ResponseBody    body  = aResponse.body();

        @NotNull byte[] bytes;
        try {
            bytes = body != null ? body.bytes() : new byte[0];
        } catch (IOException e) {
            throw new HttpReadException("Cannot read byte", e);
        }

        return new HttpResponse(aResponse.code(), aResponse.message(), convertHeaders(aResponse), bytes);
    }

    @NotNull
    private List<HttpHeader> convertHeaders(Response aResponse) {
        Headers headers = aResponse.headers();
        List<HttpHeader> list = new ArrayList<>();
        for (Pair<? extends String, ? extends String> header : headers) {
            list.add(new HttpHeader(header.getFirst(), header.getSecond()));
        }
        return list;
    }

    @NotNull
    private Request createRequest(HttpRequest aRequest) {
        Request.Builder builder = new Request.Builder()
                .url(aRequest.getUrl())
                .headers(createHeaders(aRequest.getHeaders()));

        if(aRequest.getBody() != null && aRequest.getBody().length > 0) {
            builder.method(aRequest.getMethod().name(), RequestBody.create(aRequest.getBody()));
        } else {
            builder.method(aRequest.getMethod().name(), null);
        }

        return builder.build();
    }

    @NotNull
    private OkHttpClient createClient(HttpRequestParameters aRequestParameters) {
        HttpTimeouts        timeouts        = aRequestParameters.getTimeouts();
        HttpProxyParameters proxyParameters = aRequestParameters.getProxyParameters();

        OkHttpClient.Builder builder = defaultClient.newBuilder()
                .connectTimeout ( timeouts.getConnectTimeoutMs(), MILLISECONDS )
                .readTimeout    ( timeouts.getReadTimeoutMs()   , MILLISECONDS )
                .callTimeout    ( timeouts.getCallTimeoutMs()   , MILLISECONDS )
                .writeTimeout   ( timeouts.getWriteTimeoutMs()  , MILLISECONDS );

        if(proxyParameters == null || proxyParameters.getProxy() == null) {
            return builder.build();
        }

        builder.proxy(proxyParameters.getProxy());

        if(proxyParameters.getProxyUsername() == null) {
            return builder.build();
        }

        builder.proxyAuthenticator(new ProxyAuthenticator(proxyParameters.getProxyUsername(), proxyParameters.getProxyPassword()));

        return builder.build();
    }

    private Headers createHeaders(HttpHeaders aRequestHeaders) {
        if(aRequestHeaders == null) {
            return Util.EMPTY_HEADERS;
        }
        
        List<HttpHeader> list = aRequestHeaders.asList();
        Map<String, String> map = new HashMap<>();
        for (HttpHeader header : list) {
            map.put(header.getName(), header.getValue());
        }
        return Headers.of(map);
    }

}
