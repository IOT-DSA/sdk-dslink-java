package org.dsa.iot.dslink.util.http;

import org.dsa.iot.dslink.provider.HttpProvider;
import org.dsa.iot.dslink.util.URLInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class HttpClient {

    private final URLInfo url;

    public HttpClient(URLInfo url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        this.url = url;
    }

    public HttpResp post(String uri, String content) {
        HttpProvider provider = HttpProvider.getProvider();
        Map<String, String> headers = new HashMap<>();
        {
            headers.put("connection", "close");
            headers.put("accept-encoding", "text/plain");
            headers.put("host", url.host);
        }

        URLInfo tmp = createUrlFromUri(url, uri);
        return provider.post(tmp, content, headers);
    }

    private static URLInfo createUrlFromUri(URLInfo url, String uri) {
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        return new URLInfo(url.protocol, url.host, url.port, uri, url.secure);
    }
}
