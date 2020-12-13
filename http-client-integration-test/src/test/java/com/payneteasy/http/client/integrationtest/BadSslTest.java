package com.payneteasy.http.client.integrationtest;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;
import com.payneteasy.http.client.impl.HttpClientImpl;
import com.payneteasy.http.client.okhttp.HttpClientOkHttpImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.payneteasy.http.client.api.HttpMethod.GET;

@RunWith(Parameterized.class)
public class BadSslTest {

    public static final HttpTimeouts          TIMEOUTS = new HttpTimeouts(10_000, 10_000);
    private final       IHttpClient           client;
    public static final HttpRequestParameters PARAMS   = HttpRequestParameters.builder().timeouts(TIMEOUTS).build();

    @Parameterized.Parameters
    public static IHttpClient[] parameters() {
        return new IHttpClient[]{
                new HttpClientOkHttpImpl(), new HttpClientImpl()
        };
    }

    public BadSslTest(IHttpClient client) {
        this.client = client;
    }

    @Test
    public void expired() {
        checkConnectionError("Bad ssl certificate at https://expired.badssl.com/", "https://expired.badssl.com/");
    }

    @Test
    public void wrong_host() {
        checkConnectionError("Bad ssl certificate at https://wrong.host.badssl.com/", "https://wrong.host.badssl.com/");
    }

    @Test
    public void self_signed() {
        checkConnectionError("Bad ssl certificate at https://self-signed.badssl.com/", "https://self-signed.badssl.com/");
    }

    @Test
    public void tls_v1_2() {
        checkOk("https://tls-v1-2.badssl.com:1012/");
    }

    @Test
    public void hsts() {
        checkOk("https://hsts.badssl.com/");
    }

    @Test
    public void upgrade() {
        checkOk("https://upgrade.badssl.com/");
    }

    @Test
    public void preloaded_hsts() {
        checkOk("https://preloaded-hsts.badssl.com/");
    }

    @Test
    public void https_everywhere() {
        checkOk("https://https-everywhere.badssl.com/");
    }

    @Test
    public void long_extended_subdomain_name_containing_many_letters_and_dashes() {
        checkOk("https://long-extended-subdomain-name-containing-many-letters-and-dashes.badssl.com/");
    }

    @Test
    public void longextendedsubdomainnamewithoutdashesinordertotestwordwrapping() {
        checkOk("https://longextendedsubdomainnamewithoutdashesinordertotestwordwrapping.badssl.com/");
    }

    @Test
    public void extended_validation() {
        checkOk("https://extended-validation.badssl.com/");
    }

    @Test
    public void sha512() {
        checkOk("https://sha512.badssl.com/");
    }

    @Test
    @Ignore("Error at github actions with open jdk 1.8")
    public void _10000_sans() {
        checkOk("https://10000-sans.badssl.com/");
    }

    @Test
    public void _1000_sans() {
        checkOk("https://1000-sans.badssl.com/");
    }

    @Test
    public void ecc384() {
        checkOk("https://ecc384.badssl.com/");
    }

    @Test
    public void rsa8192() {
        checkOk("https://rsa8192.badssl.com/");
    }

    private void checkOk(String aUrl) {
        long started = System.currentTimeMillis();
        System.out.print(client.getClass().getSimpleName() + " connecting to " + aUrl + " ...");

        HttpRequest request = HttpRequest.builder().method(GET).url(aUrl).build();

        try {
            HttpResponse response = client.send(request, PARAMS);
            Assert.assertEquals(200, response.getStatusCode());
            System.out.println("   OK " + (System.currentTimeMillis() - started) + " ms");
        } catch (HttpConnectException | HttpReadException | HttpWriteException e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown");
        }
    }

    private void checkConnectionError(String aError, String aUrl) {
        long started = System.currentTimeMillis();
        System.out.print(client.getClass().getSimpleName() + " connecting to " + aUrl + " ...");

        HttpRequest request = HttpRequest.builder().method(GET).url(aUrl).build();

        try {
            HttpResponse response = client.send(request, PARAMS);
            Assert.fail("Should be exception but not ok: " + response);
        } catch (HttpConnectException e) {
            Assert.assertEquals(aError, e.getMessage());
            System.out.println("   OK " + (System.currentTimeMillis() - started) + " ms");
        } catch (HttpReadException | HttpWriteException e) {
            e.printStackTrace();
            Assert.fail("Exception should be HttpConnectException");
        }
    }
}
