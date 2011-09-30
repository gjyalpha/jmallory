package org.jmallory.util;

public class TextFormatter {
    private static String       indent         = "   ";
    private static int          indentLength   = 3;

    private static final String carriageReturn = System.getProperty("line.separator");
    private static final int    returnLength   = System.getProperty("line.separator").length();

    static {
        boolean spaces = true;
        int numSpaces = 4;
        indentLength = ((spaces) ? numSpaces : 1);
        if (spaces) {
            StringBuffer strBuf = new StringBuffer();
            for (int i = 0; i < numSpaces; ++i) {
                strBuf.append(" ");
            }
            indent = strBuf.toString();
        } else {
            indent = "\t";
        }
    }

    public static String unifyText(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder doc = new StringBuilder();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0, length = text.length(); i < length; i++) {
            char ch = text.charAt(i);

            if (inString) {
                // in a string, counting escape and "
                switch (ch) {
                    case '\\':
                        escape = !escape;
                        doc.append(ch);
                        break;
                    case '"':
                        inString = escape;
                    default:
                        escape = false;
                        doc.append(ch);
                        break;
                }
            } else {
                // not in a string
                switch (ch) {
                    // ignore all spaces
                    case '\r':
                    case '\n':
                    case '\t':
                    case ' ':
                        break;
                    case '"':
                        inString = true;
                    default:
                        doc.append(ch);
                        break;
                }
            }
        }

        return doc.toString();
    }

    public static String formatText(String text) {
        StringBuilder doc = new StringBuilder(text);
        try {
            int indentCount = 0;
            boolean key = false;

            int i = 0;
            while (i < doc.length()) {
                char ch = doc.charAt(i);
                if ((ch == '"') && (doc.charAt(i - 1) != '\\')) {
                    key = !key;
                }

                if (key) {
                    ++i;
                } else {
                    switch (ch) {
                        case '{':
                            doc.replace(i, i + 1, "{" + carriageReturn + indent(++indentCount));
                            i += returnLength + indentCount * indentLength;

                            break;
                        case '}':
                            doc.replace(i, i + 1, carriageReturn + indent(--indentCount) + "}");
                            i += returnLength + indentCount * indentLength;

                            if ((doc.charAt(i + 1) != ',') && (notBracket(doc, i + 1))) {
                                doc.replace(i + 1, i + 1, carriageReturn + indent(indentCount));
                                i += returnLength + indentCount * indentLength;
                            }
                            break;
                        case ',':
                            doc.replace(i, i + 1, "," + carriageReturn + indent(indentCount));
                            i += returnLength + indentCount * indentLength;
                            break;
                        case '[':
                            doc.replace(i, i + 1, "[" + carriageReturn + indent(++indentCount));
                            i += returnLength + indentCount * indentLength;
                            break;
                        case ']':
                            doc.replace(i, i + 1, carriageReturn + indent(--indentCount) + "]");
                            i += returnLength + indentCount * indentLength;

                            if ((doc.charAt(i + 1) != ',') && (notBracket(doc, i + 1))) {
                                doc.replace(i + 1, i + 1, carriageReturn + indent(indentCount));
                                i += returnLength + indentCount * indentLength;
                            }
                            break;
                        case ' ':
                            doc.replace(i, i + 1, "");
                            --i;
                            break;
                        case '\n':
                            doc.replace(i, i + 1, "");
                            --i;
                            break;
                        case '\r':
                            doc.replace(i, i + 1, "");
                            --i;
                            break;
                        case '\t':
                            doc.replace(i, i + 1, "");
                            --i;
                            break;
                        case '\\':
                            if (doc.charAt(i + 1) == '"') {
                                ++i;
                            }
                    }

                    ++i;
                }
            }
        } catch (IndexOutOfBoundsException localIndexOutOfBoundsException) {
        }
        return doc.toString();
    }

    private static boolean notBracket(StringBuilder doc, int pos) {
        int i = pos;
        while (i < doc.length()) {
            if ((doc.charAt(i) != ' ') && (doc.charAt(i) != '\n') && (doc.charAt(i) != '\r')) {
                return (doc.charAt(i) != '}') && (doc.charAt(i) != ']');
            }

            doc.replace(i, i + 1, "");
            --i;
            ++i;
        }

        return false;
    }

    private static String indent(int count) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < count; ++i) {
            ret.append(indent);
        }
        return ret.toString();
    }

    public static String literalizeString(String rawString) {
        if (rawString == null || rawString.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = rawString.length(); i < length; i++) {
            char ch = rawString.charAt(i);

            switch (ch) {
                case '\\':
                case '"':
                case '\'':
                    sb.append('\\').append(ch);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }

        return sb.toString();
    }

    public static String deliteralizeString(String stringLiteral) {
        if (stringLiteral == null || stringLiteral.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = stringLiteral.length(); i < length; i++) {
            char ch = stringLiteral.charAt(i);
            if (ch == '\\' && i + 1 < length) {
                ch = stringLiteral.charAt(i + 1);
                i++;
                switch (ch) {
                    case '\\':
                    case '"':
                    case '\'':
                        sb.append(ch);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    default:
                        // we don't know this \x, just keep it as is
                        sb.append('\\').append(ch);
                        break;
                }

            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public static String literalizeGroovy(String rawString) {
        if (rawString == null || rawString.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = rawString.length(); i < length; i++) {
            char ch = rawString.charAt(i);

            switch (ch) {
                case '\\':
                    //                case '"':
                    //                case '\'':
                    sb.append('\\').append(ch);
                    break;
                case '\b':
                    sb.append("\\b");
                    //                    break;
                    //                case '\t':
                    //                    sb.append("\\t");
                    //                    break;
                    //                case '\n':
                    //                    sb.append("\\n");
                    //                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '$':
                    sb.append("\\$");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }

        return sb.toString();
    }

    public static String deliteralizeGroovy(String stringLiteral) {
        if (stringLiteral == null || stringLiteral.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = stringLiteral.length(); i < length; i++) {
            char ch = stringLiteral.charAt(i);
            if (ch == '\\' && i + 1 < length) {
                ch = stringLiteral.charAt(i + 1);
                i++;
                switch (ch) {
                    case '\\':
                    case '"':
                    case '\'':
                        sb.append(ch);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '$':
                        sb.append('$');
                        break;
                    default:
                        // we don't know this \x, just keep it as is
                        sb.append('\\').append(ch);
                        break;
                }

            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public static String literalizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = json.length(); i < length; i++) {
            char ch = json.charAt(i);
            if (ch == '\\' || ch == '"' || ch == '/') {
                sb.append('\\');
            }
            sb.append(ch);
        }

        return sb.toString();
    }

    public static String deliteralizeJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = jsonString.length(); i < length; i++) {
            char ch = jsonString.charAt(i);
            if (ch == '\\' && i + 1 < length) {
                ch = jsonString.charAt(i + 1);
                i++;
                // we need to ignore all escapes except \ and "
                if (ch == 'n' || ch == 'r' || ch == 't') {
                    continue;
                }
            }
            sb.append(ch);
        }

        return sb.toString();
    }

    public static String formatXml(String xml) {
        if (xml == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        int tabWidth = 4;
        int previousIndent = -1;
        int nextIndent = -1;
        int thisIndent = -1;
        int length = xml.length();
        for (int i = 0; i < length; i++) {
            // Except when we're at EOF, saved last char
            if (i + 1 == length) {
                break;
            }
            thisIndent = -1;
            char ch = xml.charAt(i);
            if ((ch == '<') && (xml.charAt(i + 1) != '/')) {
                previousIndent = nextIndent++;
                thisIndent = nextIndent;
            } else if ((ch == '<') && (xml.charAt(i + 1) == '/')) {
                if (previousIndent > nextIndent) {
                    thisIndent = nextIndent;
                }
                previousIndent = nextIndent--;
            } else if ((ch == '/') && (xml.charAt(i + 1) == '>')) {
                previousIndent = nextIndent--;
            } else if (thisIndent != -1) {
                if (thisIndent > 0) {
                    result.append('\n');
                }
                for (int j = tabWidth * thisIndent; j > 0; j--) {
                    result.append(' ');
                }
            }
            if (ch != '\n' && ch != '\r') {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
