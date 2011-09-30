package org.jmallory.http;

public class HttpResponse extends HttpMessage {

    private static final long serialVersionUID = -5139864342859890992L;

    protected String          responseLine;

    public String getResponseLine() {
        return responseLine;
    }

    public void setResponseLine(String responseLine) {
        this.responseLine = responseLine;
    }

    @Override
    protected String getStartLine(boolean urlDecode) {
        return responseLine + "\r\n";
    }

    @Override
    public void normalizeContentLength() {
        doNormalizeContentLength(false);
    }
}
