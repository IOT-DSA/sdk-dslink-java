package org.dsa.iot.dslink.methods.responses;

/**
 * Any method response can have an attached error response. An error may occur
 * during any request.
 *
 * @author Samuel Grenier
 */
public class ErrorResponse {

    private final String msg;
    private final String detail;

    public ErrorResponse(String msg, String detail) {
        this.msg = msg;
        this.detail = detail;
    }

    @SuppressWarnings("unused")
    public String getMessage() {
        return msg;
    }

    @SuppressWarnings("unused")
    public String getDetail() {
        return detail;
    }
}
