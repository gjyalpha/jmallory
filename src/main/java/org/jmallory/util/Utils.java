package org.jmallory.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.jmallory.http.HttpMessage;
import org.jmallory.http.HttpRequest;
import org.jmallory.http.HttpResponse;
import org.jmallory.io.LineInputStream;

public class Utils {

    public static final String CRLF                        = "\r\n";
    public static final String LF                          = "\n";

    public static final String CHARSET                     = "UTF-8";
    public static final byte[] EMPTY_BYTE_ARRAY            = new byte[0];

    public static final int    CONNECTION_SHRINK_THRESHOLD = 200;
    public static final int    CONNECTION_SHRINK_TO        = 100;

    public static String stripRequest(String request) throws Exception {
        int urlStart = request.indexOf(' ');
        int urlEnd = request.indexOf(' ', urlStart + 1);

        int requestStart = request.indexOf("jsonparams");

        return "URL     : " + request.substring(urlStart, urlEnd) + "\r\nrequest : "
                + URLDecoder.decode(request.substring(requestStart), "UTF-8");
    }

    public static String stripResponse(String response) {
        int responseStart = response.indexOf("{\"rsp\":");

        return "response: " + response.substring(responseStart) + "\r\n\r\n";
    }

    public static HttpRequest readRequest(LineInputStream reader) throws IOException {
        HttpRequest request = new HttpRequest();
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("invalid request line := " + line);
        }
        int methodEnd = line.indexOf(' ');
        if (methodEnd == -1) {
            throw new IOException("invalid request line := " + line);
        }
        int urlEnd = line.lastIndexOf(' ');
        if (urlEnd == -1) {
            throw new IOException("invalid request line := " + line);
        }

        readMessage(request, reader);

        // this must been call after readMessage()
        request.setRequestLine(line.substring(0, methodEnd), line.substring(methodEnd + 1, urlEnd),
                line.substring(urlEnd + 1));

        return request;
    }

    public static HttpResponse readResponse(LineInputStream reader) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setResponseLine(reader.readLine());
        readMessage(response, reader);
        return response;
    }

    public static HttpResponse constructResponse200(String content) {
        HttpResponse response = new HttpResponse();
        response.setResponseLine("HTTP/1.1 200 OK");

        return constructResponseInternal(content, response, HttpMessage.HTTP_CONTENT_TYPE_JSON);
    }

    public static HttpResponse constructResponse500(String content) {
        HttpResponse response = new HttpResponse();
        response.setResponseLine("HTTP/1.1 500 Internal Server Error");

        return constructResponseInternal(content, response, HttpMessage.HTTP_CONTENT_TYPE_TEXT);
    }

    public static HttpResponse constructResponse400(String content) {
        HttpResponse response = new HttpResponse();
        response.setResponseLine("HTTP/1.1 400 Page Not Found");

        return constructResponseInternal(content, response, HttpMessage.HTTP_CONTENT_TYPE_TEXT);
    }

    public static HttpResponse constructResponse500(Exception exception) {
        HttpResponse response = new HttpResponse();
        response.setResponseLine("HTTP/1.1 500 Internal Server Error");

        String content = null;
        if (exception != null) {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            exception.printStackTrace(print);
            content = writer.toString();
        }

        return constructResponseInternal(content, response, HttpMessage.HTTP_CONTENT_TYPE_TEXT);
    }

    private static HttpResponse constructResponseInternal(String content, HttpResponse response,
                                                          String contentType) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        response.addHeader(HttpMessage.HTTP_DATE, dateFormat.format(new Date()));
        response.addHeader(HttpMessage.HTTP_SERVER, "Apache/2.2.14 (Unix)");
        response.addHeader(HttpMessage.HTTP_CONTENT_LENGTH, "0"); // we will fix content-length in normalization
        response.addHeader(HttpMessage.HTTP_CONNECTION, HttpMessage.HTTP_CONNECTION_CLOSE); // we will fix content-length in normalization
        response.addHeader(HttpMessage.HTTP_CONTENT_TYPE, contentType != null ? contentType
                : HttpMessage.HTTP_CONTENT_TYPE_TEXT);
        if (content != null) {
            try {
                response.setData(content.getBytes(Utils.CHARSET));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                response.setData(content.getBytes());
            }
        }

        response.normalizeContentLength();

        return response;
    }

    private static void readMessage(HttpMessage message, LineInputStream reader) throws IOException {

        String line = null;
        // read headers
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                // we have done the headers, break;
                break;
            }
            int separatorIndex = line.indexOf(": ");
            if (separatorIndex == -1) {
                throw new IOException("invalid header: " + line);
            }
            String name = line.substring(0, separatorIndex);
            String value = line.substring(separatorIndex + 2);
            message.addHeader(name, value);
        }

        byte[] dataBytes = readData(reader, message);

        message.setData(dataBytes);
    }

    private static byte[] readData(LineInputStream reader, HttpMessage message) throws IOException {
        byte[] dataBytes = EMPTY_BYTE_ARRAY;
        // default reading strategy is read nothing, i.e. length == 0
        int readingType = HttpMessage.READING_TYPE_LENGTH;
        int length = 0;
        if (reader.isLengthByHeader()) {
            // check content length header
            String lengthString = message.getHeader(HttpMessage.HTTP_CONTENT_LENGTH);
            if (lengthString != null) {
                readingType = HttpMessage.READING_TYPE_LENGTH;
                length = Integer.parseInt(lengthString);
            } else {
                // check Connection: close header
                String connection = message.getHeader(HttpMessage.HTTP_CONNECTION);
                if (HttpMessage.HTTP_CONNECTION_CLOSE.equalsIgnoreCase(connection)) {
                    readingType = HttpMessage.READING_TYPE_EOF;
                    length = -1; // means read until to the end
                } else {
                    String transferEncoding = message.getHeader(HttpMessage.HTTP_TRANSFER_ENCODING);
                    if (transferEncoding != null
                            && transferEncoding.indexOf(HttpMessage.HTTP_TRANSFER_ENCODING_CHUNKED) != -1) {
                        readingType = HttpMessage.READING_TYPE_CHUNKED;
                        length = -1;
                    }
                }
            }
        } else {
            // we use available to read all data
            readingType = HttpMessage.READING_TYPE_LENGTH;
            length = reader.available();
        }

        switch (readingType) {
            case HttpMessage.READING_TYPE_LENGTH: {
                // read by length
                int saved = 0;
                dataBytes = (length == 0 ? EMPTY_BYTE_ARRAY : new byte[length]);
                while (length != 0) {
                    int read = reader.read(dataBytes, saved, length);
                    if (read == -1) {
                        // we reached the end of the stream
                        break;
                    }
                    saved += read;
                    length -= read;
                }
                break;
            }
            case HttpMessage.READING_TYPE_CHUNKED: {
                /**
                 * <pre>
                 * RFC's chunk parsing flow:
                 *   　　length := 0
                 *   　　read chunk-size, chunk-ext (if any) and CRLF
                 *   　　while (chunk-size > 0) {
                 *   　　read chunk-data and CRLF
                 *   　　append chunk-data to entity-body
                 *   　　length := length + chunk-size
                 *   　　read chunk-size and CRLF
                 *   　　}
                 *   　　read entity-header
                 *   　　while (entity-header not empty) {
                 *   　　append entity-header to existing header fields
                 *   　　read entity-header
                 *   　　}
                 *   　　Content-Length := length
                 *   　　Remove "chunked" from Transfer-Encoding
                 * </pre>
                 */
                length = 0;
                String chunkSize = reader.readLine();
                if (chunkSize == null) {
                    break;
                }
                // remove chunk-ext
                int extIndex = chunkSize.indexOf(';');
                if (extIndex != -1) {
                    chunkSize.substring(0, extIndex);
                }
                int size = Integer.parseInt(chunkSize, 16);
                // read
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (size > 0) {
                    for (int i = 0; i < size; i++) {
                        baos.write(reader.read());
                    }
                    reader.readLine();
                    length += size;
                    chunkSize = reader.readLine();
                    if (chunkSize != null) {
                        size = Integer.parseInt(chunkSize, 16);
                    } else {
                        size = 0;
                    }
                }
                // populate header
                message.addHeader(HttpMessage.HTTP_CONTENT_LENGTH, String.valueOf(length));
                message.markHeaderAsXorig(HttpMessage.HTTP_TRANSFER_ENCODING);
                dataBytes = baos.toByteArray();
                break;
            }
            case HttpMessage.READING_TYPE_EOF:
                // fall through to the default reading, i.e. EOF
            default: {
                // read to the end
                dataBytes = readAllToBytes(reader);
                break;
            }
        }

        if (HttpMessage.HTTP_CONTENT_ENCODING_GZIP.equalsIgnoreCase(message
                .getHeader(HttpMessage.HTTP_CONTENT_ENCODING))
                && dataBytes != null
                && dataBytes.length != 0) {
            // the content is gziped, let's unzip it
            dataBytes = readAllToBytes(new GZIPInputStream(new ByteArrayInputStream(dataBytes)));
            // and we remove the content encoding header
            message.markHeaderAsXorig(HttpMessage.HTTP_CONTENT_ENCODING);
            message.addHeader(HttpMessage.HTTP_CONTENT_LENGTH, String.valueOf(dataBytes.length));
        }

        return dataBytes;
    }

    public static byte[] readAllToBytes(InputStream in) throws IOException {
        if (in == null) {
            return EMPTY_BYTE_ARRAY;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read = -1;
        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        if (baos.size() == 0) {
            return EMPTY_BYTE_ARRAY;
        } else {
            return baos.toByteArray();
        }
    }

    public static void encodeRequest(HttpRequest request) {
        try {
            request.setRequestLine(request.getMethod(),
                    URLEncoder.encode(request.getRequestUrl(), Utils.CHARSET),
                    request.getHttpVersion());
            request.setData(URLEncoder.encode(new String(request.getData(), Utils.CHARSET),
                    Utils.CHARSET).getBytes(Utils.CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void decodeRequest(HttpRequest request) {
        try {
            request.setRequestLine(request.getMethod(),
                    URLDecoder.decode(request.getRequestUrl(), Utils.CHARSET),
                    request.getHttpVersion());
            request.setData(URLDecoder.decode(new String(request.getData(), Utils.CHARSET),
                    Utils.CHARSET).getBytes(Utils.CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void chmod777(File file) {
        if (file == null) {
            return;
        }

        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
    }
}
