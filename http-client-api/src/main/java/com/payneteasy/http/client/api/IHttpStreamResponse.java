package com.payneteasy.http.client.api;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IHttpStreamResponse extends Closeable {

    int getStatusCode();

    @Nonnull
    String getReasonPhrase();

    @Nonnull
    List<HttpHeader> getHeaders();

    @Nonnull
    InputStream getInputStream() throws IOException;

}
