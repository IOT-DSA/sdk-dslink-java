package org.dsa.iot.dslink.util.http;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Samuel Grenier
 */
public class HttpResp {

    private HttpResponseStatus status;
    private String body;

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setStatus(HttpResponseStatus status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
