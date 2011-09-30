package org.jmallory.http;

import java.io.Serializable;

public class ReqRespData implements Comparable<ReqRespData>, Serializable {
    
    private static final long serialVersionUID = 7398810021570139023L;
    
    private HttpRequest  request;
    private HttpResponse response;

    public ReqRespData() {
    }
    
    public ReqRespData(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReqRespData)) {
            return false;
        }
        ReqRespData another = (ReqRespData) obj;
        if (another.request == null || another.request == null) {
            return false;
        }

        return request.equals(another.request);
    }

    @Override
    public int hashCode() {
        return request.hashCode();
    }

    @Override
    public String toString() {
        return request.toString();
    }

    @Override
    public int compareTo(ReqRespData o) {
        return request.compareTo(o.request);
    }

}
