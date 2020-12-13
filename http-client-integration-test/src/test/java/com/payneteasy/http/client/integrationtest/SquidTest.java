package com.payneteasy.http.client.integrationtest;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;
import com.payneteasy.http.client.impl.HttpClientImpl;
import com.payneteasy.http.client.okhttp.HttpClientOkHttpImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static java.net.Proxy.Type.HTTP;

@RunWith(Parameterized.class)
public class SquidTest {

    private final       IHttpClient           client;

    public SquidTest(IHttpClient client) {
        this.client = client;
    }

    static {
        HttpClientImpl.registerGlobalProxyAuthenticator();
    }
    
    @Parameterized.Parameters
    public static IHttpClient[] parameters() {
        return new IHttpClient[]{new HttpClientOkHttpImpl(), new HttpClientImpl()};
    }

    @Test
    public void connection_refused() throws HttpConnectException, HttpReadException, HttpWriteException {

        HttpRequest request  = HttpRequest.builder()
                .method(HttpMethod.GET)
                .url("https://checkip.amazonaws.com")
                .headers(new HttpHeadersBuilder()
                        .add("User-Agent", "my agent")
                        .build())
                .build();
        HttpProxyParameters proxy = new HttpProxyParameters(
                new Proxy(HTTP, new InetSocketAddress("127.0.0.1", 3128))
                , "username-1"
                , "password-1");

        HttpRequestParameters params   = HttpRequestParameters.builder()
                .timeouts(new HttpTimeouts(10_000, 10_000))
                .proxyParameters(proxy)
                .build();


        try {
            HttpResponse response = client.send(request, params);
            System.out.println("response = " + response);
            Assert.fail("HttpConnectException should be thrown");
        } catch (HttpConnectException e) {
            Assert.assertTrue(e.getMessage().contains("https://checkip.amazonaws.com"));
        } catch (HttpReadException | HttpWriteException e) {
            e.printStackTrace();
            Assert.fail("Exception should be HttpConnectException");
        }

    }
}
