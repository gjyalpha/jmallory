package org.jmallory.http;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jmallory.util.TextFormatter;
import org.jmallory.util.Utils;

public abstract class HttpMessage implements Serializable {

    private static final long  serialVersionUID                  = -1082382102520846559L;

    public static final String HTTP_PROTOCOL_PREFIX              = "http://";
    public static final String HTTPS_PROTOCOL_PREFIX             = "https://";

    public static final char   HTTP_PORT_SEPARATOR               = ':';
    public static final char   HTTP_PARAMETER_SEPARATOR          = ';';

    public static final String HTTP_DATE                         = "Date";
    public static final String HTTP_SERVER                       = "Server";

    public static final String HTTP_CONNECTION                   = "Connection";
    public static final String HTTP_CONNECTION_CLOSE             = "close";
    public static final String HTTP_CONTENT_ENCODING             = "Content-Encoding";
    public static final String HTTP_CONTENT_ENCODING_GZIP        = "gzip";
    public static final String HTTP_CONTENT_LENGTH               = "Content-Length";
    public static final String HTTP_CONTENT_TYPE                 = "Content-Type";
    public static final String HTTP_CONTENT_TYPE_JSON            = "application/json";
    public static final String HTTP_CONTENT_TYPE_TEXT            = "text/plain";
    public static final String HTTP_CONTENT_TYPE_MIME_URL_ENCODE = "application/x-www-form-urlencoded";
    public static final String HTTP_CONTENT_TYPE_CHARSET         = "charset";
    public static final String HTTP_CONTENT_TYPE_CHARSET_EQUAL   = "charset=";
    public static final String HTTP_TRANSFER_ENCODING            = "Transfer-Encoding";
    public static final String HTTP_TRANSFER_ENCODING_CHUNKED    = "chunked";

    public static final String HTTP_COOKIE                       = "Cookie";
    public static final String HTTP_SET_COOKIE                   = "Set-Cookie";

    public static final String HTTP_HOST                         = "Host";

    public static final String HTTP_X_ORIG_PREFIX                = "X-Orig-AM-";

    public static final int    READING_TYPE_EOF                  = -1;
    public static final int    READING_TYPE_LENGTH               = 0;
    public static final int    READING_TYPE_CHUNKED              = 1;

    public static Set<String>  MERGABLE_HEADER;
    {
        HashSet<String> mergableHeaders = new HashSet<String>();
        mergableHeaders.add(HTTP_COOKIE.toLowerCase());
        mergableHeaders.add(HTTP_SET_COOKIE.toLowerCase());

        MERGABLE_HEADER = Collections.unmodifiableSet(mergableHeaders);
    }

    public static class HttpHeader implements Serializable {
        private static final long serialVersionUID = 1L;

        public String             name;
        public String             value;

        public HttpHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return name + ": " + value;
        }
    }

    protected List<HttpHeader> headerList;
    protected byte[]           data;
    protected String           dataString;

    public List<HttpHeader> getHeaderList() {
        return headerList;
    }

    public void setHeaderList(List<HttpHeader> headerList) {
        this.headerList = headerList;
    }

    public String getHeader(String name) {
        if (name == null || headerList == null) {
            return null;
        }

        for (HttpHeader header : headerList) {
            if (name.equalsIgnoreCase(header.name)) {
                return header.value;
            }
        }

        return null;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        try {
            if (data == null || data.length == 0) {
                dataString = "";
            } else {
                String contentType = this.getHeader(HTTP_CONTENT_TYPE);
                if (contentType == null) {
                    // not content type, use default encoding to string
                    this.dataString = new String(data, Utils.CHARSET);
                } else {
                    // has content type, is there any charset info?
                    contentType = contentType.toLowerCase();
                    int charsetIndex = contentType.indexOf(HTTP_CONTENT_TYPE_CHARSET_EQUAL);
                    if (charsetIndex == -1) {
                        // no charset info, use default encoding to string
                        this.dataString = new String(data, Utils.CHARSET);
                    } else {
                        int charsetValueIndex = charsetIndex
                                + HTTP_CONTENT_TYPE_CHARSET_EQUAL.length();
                        StringBuilder tokenSb = new StringBuilder();
                        while (charsetValueIndex < contentType.length()) {
                            char ch = contentType.charAt(charsetValueIndex);
                            if (Character.isWhitespace(ch) || HTTP_PARAMETER_SEPARATOR == ch) {
                                // end of charset value
                                break;
                            } else {
                                // part of charset value
                                tokenSb.append(ch);
                            }
                            charsetValueIndex++;
                        }

                        if (tokenSb.length() == 0) {
                            // no valid charset info, use default encoding to string
                            this.dataString = new String(data, Utils.CHARSET);
                        } else {
                            try {
                                Charset charset = Charset.forName(tokenSb.toString());
                                this.dataString = new String(data, charset);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.dataString = new String(data, Utils.CHARSET);
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String toString(boolean formatDataAsJson) {
        return toHeaderString(false).append(
                formatDataAsJson ? TextFormatter.formatText(dataString) : dataString).toString();
    }

    public String toString() {
        return toString(false);
    }

    public String toUrlDecodedString(boolean formatDataAsJson) {
        String contentType = getHeader(HTTP_CONTENT_TYPE);
        if (contentType != null && contentType.startsWith(HTTP_CONTENT_TYPE_MIME_URL_ENCODE)) {
            try {
                return toHeaderString(true).append(
                        formatDataAsJson ? TextFormatter.formatText(URLDecoder.decode(dataString,
                                Utils.CHARSET)) : URLDecoder.decode(dataString, Utils.CHARSET))
                        .toString();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                return dataString + "\n\n" + e.getMessage();
            }
        } else {
            return toHeaderString(true).append(
                    formatDataAsJson ? TextFormatter.formatText(dataString) : dataString)
                    .toString();
        }
    }

    public String toUrlDecodedString() {
        return toUrlDecodedString(true);
    }

    public byte[] toByteArray() {
        byte[] bytes = null;
        try {
            byte[] header = toHeaderString(false).toString().getBytes(Utils.CHARSET);
            bytes = new byte[header.length + data.length];
            System.arraycopy(header, 0, bytes, 0, header.length);
            System.arraycopy(data, 0, bytes, header.length, data.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    protected StringBuilder toHeaderString(boolean decodeUrl) {
        StringBuilder sb = new StringBuilder(getStartLine(decodeUrl));
        if (headerList != null) {
            for (HttpHeader header : headerList) {
                sb.append(header.name).append(": ").append(header.value).append("\r\n");
            }
        }
        sb.append("\r\n");
        return sb;
    }

    protected abstract String getStartLine(boolean decodeUrl);

    public String getDataString() {
        return dataString;
    }

    public String getUrlDecodedDataString() {
        if (dataString == null) {
            return null;
        }

        try {
            return URLDecoder.decode(dataString, Utils.CHARSET).toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return dataString;
        }
    }

    public abstract void normalizeContentLength();

    protected void doNormalizeContentLength(boolean trim) {

        // does the user specify the content-length mandatorily
        String value = getHeader(HTTP_CONTENT_LENGTH);
        if (value != null) {
            if (value.endsWith("x") || value.endsWith("l")) {
                addHeader(HttpMessage.HTTP_CONTENT_LENGTH, value.substring(0, value.length() - 1));
                return;
            }
        }

        if (this.getData().length != 0) {
            if (trim) {
                try {
                    this.setData(this.getDataString().trim().getBytes(Utils.CHARSET));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            // In Tomcat, if Connection: close, there must be Content-length as well, otherwise Tomcat can't read the response.
            // set content-length
            calculateAndSetContentLengthHeaderAccordingToData();
            // so let's don't deal with Connection header
            //            String connection = getHeader(HTTP_CONNECTION);
            //            if (HTTP_CONNECTION_CLOSE.equals(connection)) {
            //                // we got connection close header, remove the content-length header if any
            //                removeHeader(HttpMessage.HTTP_CONTENT_LENGTH);
            //            } else {
            //                // set content-length
            //                calculateAndSetContentLengthHeaderAccordingToData();
            //            }

        } else {
            removeHeader(HttpMessage.HTTP_CONTENT_LENGTH);
        }
    }

    private void calculateAndSetContentLengthHeaderAccordingToData() {
        addHeader(HttpMessage.HTTP_CONTENT_LENGTH, String.valueOf(this.getData().length));
    }

    /**
     * Returns true if removed successfully
     * 
     * @param name header name to remove
     * @return
     */
    public boolean removeHeader(String name) {
        if (name == null || headerList == null) {
            return false;
        }
        boolean removed = false;
        for (Iterator<HttpHeader> iterator = headerList.iterator(); iterator.hasNext();) {
            HttpHeader header = iterator.next();
            if (name.equalsIgnoreCase(header.name)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Marks a header with "x-orig-" prefix.
     * 
     * @param name
     * @return true if marked successfully
     */
    public boolean markHeaderAsXorig(String name) {
        if (name == null || headerList == null) {
            return false;
        }
        boolean marked = false;
        for (int i = 0, length = headerList.size(); i < length; i++) {
            HttpHeader header = headerList.get(i);
            if (name.equalsIgnoreCase(header.name)) {
                header.name = HTTP_X_ORIG_PREFIX + header.name;
                marked = true;
            }
        }
        return marked;
    }

    public void addHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }

        if (headerList == null) {
            headerList = new ArrayList<HttpHeader>();
        }

        for (HttpHeader header : headerList) {
            if (name.equalsIgnoreCase(header.name)) {
                // this header name is already used, let's see whether we need to keep/merge the value or replace the value
                if (MERGABLE_HEADER.contains(name.toLowerCase())) {
                    break; // break to add the name/value as usual
                } else {
                    // replace the value
                    header.value = value;
                    return;
                }
            }
        }

        // header not exist, append to headerList
        headerList.add(new HttpHeader(name, value));
    }
}
