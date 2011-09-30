package org.jmallory.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.jmallory.util.Utils;

public class HttpRequest extends HttpMessage implements Comparable<HttpRequest> {

    private static final long serialVersionUID = -2572278238175429233L;

    protected String          key;
    protected String          method;
    protected String          requestUrl;
    protected String          httpVersion;

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getMethod() {
        return method;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * This must been call after setData() is called
     * 
     * @param method
     * @param requestUrl
     * @param httpVersion
     */
    public void setRequestLine(String method, String requestUrl, String httpVersion) {
        try {
            this.key = method + " " + requestUrl + " " + new String(data, Utils.CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.method = method;
        this.requestUrl = requestUrl;
        this.httpVersion = httpVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HttpRequest)) {
            return false;
        }
        HttpRequest another = (HttpRequest) obj;
        return key.equals(another.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo(HttpRequest o) {
        return key.compareTo(o.key);
    }

    @Override
    protected String getStartLine(boolean decodeUrl) {
        return method + " " + requestUrl + " " + httpVersion + "\r\n";
    }

    public String getMethodRequestUrl(boolean decodeUrl) {
        if (decodeUrl) {
            try {
                return method + " " + URLDecoder.decode(requestUrl, Utils.CHARSET);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        } else {
            return method + " " + requestUrl;
        }
    }

    public String getMethodRequestUrl() {
        return getMethodRequestUrl(false);
    }

    @Override
    public void normalizeContentLength() {
        doNormalizeContentLength(true);
        // data changed, we need to reset the key
        setRequestLine(method, requestUrl, httpVersion);
    }
}
