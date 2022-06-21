# http-client

The simplest http client.

Features:
* no dependecies
* ability to run on Android
* small footprint
* URLConnection or OkHttp

## How to add it into your app

### Maven


```xml
<repositories>
    <repository>
        <id>pne</id>
        <name>payneteasy repo</name>
        <url>https://maven.pne.io</url>
    </repository>
</repositories>
  
<dependency>
    <groupId>com.payneteasy.http-client</groupId>
    <artifactId>http-client-impl</artifactId>
    <version>1.0-6</version>
</dependency>
```

### Java code

```java
HttpRequest request = HttpRequest.builder()
    .method ( GET  )
    .url    ( "https://extended-validation.badssl.com/" )
    .build();

HttpRequestParameters params = HttpRequestParameters.builder()
    .timeouts(new HttpTimeouts(10_000, 10_000))
    .build()

IHttpClient client = new HttpClientOkHttpImpl(); // or new HttpClientImpl();

HttpResponse response = client.send(request, params);

```

