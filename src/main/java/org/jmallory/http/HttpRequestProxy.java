package org.jmallory.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jmallory.util.TextFormatter;

public class HttpRequestProxy {

    private static Logger logger          = Logger.getLogger(HttpRequestProxy.class.getName());
    private static int    connectTimeOut  = 1000;

    private static int    readTimeOut     = 1000;

    private static String requestEncoding = "UTF-8";

    /**
     * Add parameters stored in the Map to the uri string. Map can contain
     * Object values which will be converted to the string, or Object arrays,
     * which will be treated as multivalue attributes.
     * 
     * @param uri The uri to add parameters into
     * @param parameters The map containing parameters to be added
     * @return The uri with added parameters
     */
    private static String parameterize(/* String uri, */Map parameters) {
        String uri = "";
        if (parameters.size() == 0) {
            return uri;
        }

        StringBuffer buffer = new StringBuffer(uri);
        // if (uri.indexOf('?') == -1) {
        // buffer.append('?');
        // } else {
        // buffer.append('&');
        // }

        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            if (entry.getValue().getClass().isArray()) {
                Object[] value = (Object[]) entry.getValue();
                for (int j = 0; j < value.length; j++) {
                    if (j > 0) {
                        buffer.append('&');
                    }
                    buffer.append(entry.getKey());
                    buffer.append('=');
                    buffer.append(replaceSpace((String) value[j]));
                }
            } else {
                buffer.append(entry.getKey());
                buffer.append('=');
                buffer.append(replaceSpace((String) entry.getValue()));
            }
            if (i.hasNext()) {
                buffer.append('&');
            }
        }
        return buffer.toString();
    }

    private static String replaceSpace(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return content.replaceAll("(\n|\r|\\s*([{}\\[\\]:,])\\s*)", "$2");
    }

    /**
     * <pre>
     * 鍙戦€佸甫鍙傛暟鐨凱OST鐨凥TTP璇锋眰
     * </pre>
     * 
     * @param reqUrl HTTP璇锋眰URL
     * @param parameters 鍙傛暟鏄犲皠琛? * @return HTTP鍝嶅簲鐨勫瓧绗︿覆
     */
    public static String doRequest(String reqUrl, Map parameters, String recvEncoding) {
        HttpURLConnection httpConn = null;
        String responseContent = null;
        String params = "";
        try {
            // reqUrl = parameterize(reqUrl, parameters);

            URL httpurl = new URL(reqUrl);
            httpConn = (HttpURLConnection) httpurl.openConnection();
            httpConn.setConnectTimeout(HttpRequestProxy.connectTimeOut);
            httpConn.setReadTimeout(HttpRequestProxy.readTimeOut);
            httpConn.setDoInput(true);

            OutputStreamWriter wr = null;
            // httpConn.setRequestMethod("POST");
            if (!parameters.keySet().isEmpty()) {
                httpConn.setDoOutput(true);
                wr = new OutputStreamWriter(httpConn.getOutputStream());
                params = parameterize(parameters);
                wr.write(params);
                wr.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream(),
                    recvEncoding));
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            String line;
            while ((line = in.readLine()) != null) {
                tempStr.append(line);
                tempStr.append(crlf);
            }
            responseContent = reqUrl + "\n\n" + params + "\n\n"
                    + TextFormatter.formatText(tempStr.toString());

            if (wr != null) {
                wr.close();
            }
            in.close();
        } catch (IOException e) {
            logger.log(Level.ALL, "network error", e);
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            responseContent = reqUrl + "\n\n" + params + "\n\n" + w.toString();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return responseContent;
    }

    public static int getConnectTimeOut() {
        return HttpRequestProxy.connectTimeOut;
    }

    public static int getReadTimeOut() {
        return HttpRequestProxy.readTimeOut;
    }

    public static String getRequestEncoding() {
        return requestEncoding;
    }

    public static void setConnectTimeOut(int connectTimeOut) {
        HttpRequestProxy.connectTimeOut = connectTimeOut;
    }

    public static void setReadTimeOut(int readTimeOut) {
        HttpRequestProxy.readTimeOut = readTimeOut;
    }

    public static void setRequestEncoding(String requestEncoding) {
        HttpRequestProxy.requestEncoding = requestEncoding;
    }
}
