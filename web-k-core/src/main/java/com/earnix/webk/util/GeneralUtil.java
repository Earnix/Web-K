/*
 * {{{ header & license
 * GeneralUtil.java
 * Copyright (c) 2004, 2005 Patrick Wright
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.earnix.webk.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Patrick Wright
 */
public class GeneralUtil {

    /**
     * Used to format an Object's hashcode into a 0-padded 10 char String, e.g.
     * for 24993066 returns "0024993066"
     */
    public final static java.text.DecimalFormat PADDED_HASH_FORMAT = new java.text.DecimalFormat("0000000000");

    private final static String JAVASCRIPT_ES6_PROPERTY = "webk.javascript.es6";

    public static InputStream openStreamFromClasspath(Object obj, String resource) {
        InputStream readStream = null;
        try {
            ClassLoader loader = obj.getClass().getClassLoader();
            if (loader == null) {
                readStream = ClassLoader.getSystemResourceAsStream(resource);
            } else {
                readStream = loader.getResourceAsStream(resource);
            }
            if (readStream == null) {
                URL stream = resource.getClass().getResource(resource);
                if (stream != null) readStream = stream.openStream();
            }
        } catch (Exception ex) {
            XRLog.exception("Could not open stream from CLASSPATH: " + resource, ex);
        }
        return readStream;
    }

    public static URL getURLFromClasspath(Object obj, String resource) {
        URL url = null;
        try {
            ClassLoader loader = obj.getClass().getClassLoader();
            if (loader == null) {
                url = ClassLoader.getSystemResource(resource);
            } else {
                url = loader.getResource(resource);
            }
            if (url == null) {
                url = resource.getClass().getResource(resource);
            }
        } catch (Exception ex) {
            XRLog.exception("Could not get URL from CLASSPATH: " + resource, ex);
        }
        return url;
    }

    /**
     * Dumps an exception to the console, only the last 5 lines of the stack
     * trace.
     */
    public static void dumpShortException(Exception ex) {
        String s = ex.getMessage();
        if (s == null || s.trim().equals("null")) {
            s = "{no ex. message}";
        }
        System.out.println(s + ", " + ex.getClass());
        StackTraceElement[] stes = ex.getStackTrace();
        for (int i = 0; i < stes.length && i < 5; i++) {
            StackTraceElement ste = stes[i];
            System.out.println("  " + ste.getClassName() + "." + ste.getMethodName() + "(ln " + ste.getLineNumber() + ")");
        }
    }

    /**
     * Returns a String tracking the last n method calls, from oldest to most
     * recent. You can use this as a simple tracing mechanism to find out the
     * calls that got to where you execute the <code>trackBack()</code> call
     * from. Example:</p>
     * <pre>
     * // called from Box.calcBorders(), line 639
     * String tback = GeneralUtil.trackBack(6);
     * System.out.println(tback);
     * </pre> produces
     * <pre>
     * Boxing.layoutChildren(ln 204)
     * BlockBoxing.layoutContent(ln 81)
     * Boxing.layout(ln 72)
     * Boxing.layout(ln 133)
     * Box.totalLeftPadding(ln 306)
     * Box.calcBorders(ln 639)
     * </pre>
     * The <code>trackBack()</code> method itself is always excluded from the dump.
     * Note the output may not be useful if HotSpot has been optimizing the
     * code.
     *
     * @param cnt How far back in the call tree to go; if call tree is smaller, will
     *            be limited to call tree.
     * @return see desc
     */
    public static String trackBack(int cnt) {
        Exception ex = new Exception();
        StringBuffer sb = new StringBuffer();
        List list = new ArrayList(cnt);
        StackTraceElement[] stes = ex.getStackTrace();
        if (cnt >= stes.length) {
            cnt = stes.length - 1;
        }

        // >= 1 to not include this method
        for (int i = cnt; i >= 1; i--) {
            StackTraceElement ste = stes[i];
            sb.append(GeneralUtil.classNameOnly(ste.getClassName()));
            sb.append(".");
            sb.append(ste.getMethodName());
            sb.append("(ln ").append(ste.getLineNumber()).append(")");
            list.add(sb.toString());
            sb = new StringBuffer();
        }

        Iterator iter = list.iterator();
        StringBuffer padding = new StringBuffer("");
        StringBuffer trackback = new StringBuffer();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            trackback.append(padding).append(s).append("\n");
            padding.append("   ");
        }
        return trackback.toString();
    }


    /**
     * Given an Object instance, returns just the classname with no package
     *
     * @param o PARAM
     * @return Returns
     */
    public static String classNameOnly(Object o) {
        String s = "[null object ref]";
        if (o != null) {
            s = classNameOnly(o.getClass().getName());
        }
        return s;
    }

    /**
     * Given a String classname, returns just the classname with no package
     *
     * @param cname PARAM
     * @return Returns
     */
    public static String classNameOnly(String cname) {
        String s = "[null object ref]";
        if (cname != null) {
            s = cname.substring(cname.lastIndexOf('.') + 1);
        }
        return s;
    }

    /**
     * Description of the Method
     *
     * @param o PARAM
     * @return Returns
     */
    public static String paddedHashCode(Object o) {
        String s = "0000000000";
        if (o != null) {
            s = PADDED_HASH_FORMAT.format(o.hashCode());
        }
        return s;
    }

    public static boolean isMacOSX() {
        try {
            if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
                return true;
            }
        } catch (SecurityException e) {
            System.err.println(e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Checks if ES6 is enabled for Javascript engine. Default: {@code true}.
     * <br>System property: {@code webk.runtime.es6}
     * @return status of boolean system property {@code webk.javascript.es6}. true - if system property is not
     * specified.
     */
    public static boolean getJavascriptES6Status()
    {
        try {
            String supportES6 = System.getProperty(JAVASCRIPT_ES6_PROPERTY, "true");
            return Boolean.valueOf(supportES6);
        } catch (SecurityException e) {
            System.err.println(e.getLocalizedMessage());
        }
        return true;
    }

    /**
     * Configure system property {@code webk.javascript.es6}. The method doesnt affect the execution javascript
     * runtime engine but only new one.
     * @param enable
     */
    public static void setJavascritES6Status(boolean enable)
    {
        System.setProperty(JAVASCRIPT_ES6_PROPERTY, String.valueOf(enable));
    }

    public static StringBuffer htmlEscapeSpace(String uri) {
        StringBuffer sbURI = new StringBuffer((int) (uri.length() * 1.5));
        char ch;
        for (int i = 0; i < uri.length(); ++i) {
            ch = uri.charAt(i);
            if (ch == ' ') {
                sbURI.append("%20");
            } else if (ch == '\\') {
                sbURI.append('/');
            } else {
                sbURI.append(ch);
            }
        }
        return sbURI;
    }

    /**
     * Reads all content from a given InputStream into a String using the default platform encoding.
     *
     * @param is the InputStream to read from. Must already be open, and will NOT be closed by this function. Failing to
     *           close this stream after the call will result in a resource leak.
     * @return String containing contents read from the stream
     * @throws IOException if the stream could not be read
     */
    public static String inputStreamToString(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringWriter sw = new StringWriter();
        char c[] = new char[1024];
        while (true) {
            int n = br.read(c, 0, c.length);
            if (n < 0) break;
            sw.write(c, 0, n);
        }
        isr.close();
        return sw.toString();
    }

    public static void writeStringToFile(String content, String encoding, String fileName)
            throws IOException {
        File f = new File(fileName);
        FileOutputStream fos = new FileOutputStream(f);
        try {
            OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
            BufferedWriter bw = new BufferedWriter(osw);
            PrintWriter pw = new PrintWriter(bw);
            try {
                pw.print(content);
                pw.flush();
                bw.flush();
            } finally {
                try {
                    pw.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                // ignore
            }
        }
        System.out.println("Wrote file: " + f.getAbsolutePath());
    }

    /**
     * Parses an integer from a string using less restrictive rules about which
     * characters we won't accept.  This scavenges the supplied string for any
     * numeric character, while dropping all others.
     *
     * @param s The string to parse
     * @return The number represented by the passed string, or 0 if the string
     * is null, empty, white-space only, contains only non-numeric
     * characters, or simply evaluates to 0 after parsing (e.g. "0")
     */
    public static int parseIntRelaxed(String s) {
        // An edge-case short circuit...
        if (s == null || s.length() == 0 || s.trim().length() == 0) {
            return 0;
        }

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                buffer.append(c);
            } else {
                // If we hit a non-numeric with numbers already in the
                // buffer, we're done.
                if (buffer.length() > 0) {
                    break;
                }
            }
        }

        if (buffer.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(buffer.toString());
        } catch (NumberFormatException exception) {
            // The only way we get here now is if s > Integer.MAX_VALUE
            return Integer.MAX_VALUE;
        }
    }

    public static int parseInt(String string, int def) {
        try {
            return Integer.parseInt(string.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Converts any special characters into their corresponding HTML entities , for example < to &lt;. This is done using a character
     * by character test, so you may consider other approaches for large documents. Make sure you declare the
     * entities that might appear in this replacement, e.g. the latin-1 entities
     * This method was taken from a code-samples website, written and hosted by Real Gagnon, at
     * http://www.rgagnon.com/javadetails/java-0306.html.
     *
     * @param s The String which may contain characters to escape.
     * @return The string with the characters as HTML entities.
     */
    public static String escapeHTML(String s) {
        if (s == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                /*
             case 'ÔøΩ': sb.append("&agrave;");break;
             case 'ÔøΩ': sb.append("&Agrave;");break;
             case 'ÔøΩ': sb.append("&acirc;");break;
             case 'ÔøΩ': sb.append("&Acirc;");break;
             case 'ÔøΩ': sb.append("&auml;");break;
             case 'ÔøΩ': sb.append("&Auml;");break;
             case 'ÔøΩ': sb.append("&aring;");break;
             case 'ÔøΩ': sb.append("&Aring;");break;
             case 'ÔøΩ': sb.append("&aelig;");break;
             case 'ÔøΩ': sb.append("&AElig;");break;
             case 'ÔøΩ': sb.append("&ccedil;");break;
             case 'ÔøΩ': sb.append("&Ccedil;");break;
             case 'ÔøΩ': sb.append("&eacute;");break;
             case 'ÔøΩ': sb.append("&Eacute;");break;
             case 'ÔøΩ': sb.append("&egrave;");break;
             case 'ÔøΩ': sb.append("&Egrave;");break;
             case 'ÔøΩ': sb.append("&ecirc;");break;
             case 'ÔøΩ': sb.append("&Ecirc;");break;
             case 'ÔøΩ': sb.append("&euml;");break;
             case 'ÔøΩ': sb.append("&Euml;");break;
             case 'ÔøΩ': sb.append("&iuml;");break;
             case 'ÔøΩ': sb.append("&Iuml;");break;
             case 'ÔøΩ': sb.append("&ocirc;");break;
             case 'ÔøΩ': sb.append("&Ocirc;");break;
             case 'ÔøΩ': sb.append("&ouml;");break;
             case 'ÔøΩ': sb.append("&Ouml;");break;
             case 'ÔøΩ': sb.append("&oslash;");break;
             case 'ÔøΩ': sb.append("&Oslash;");break;
             case 'ÔøΩ': sb.append("&szlig;");break;
             case 'ÔøΩ': sb.append("&ugrave;");break;
             case 'ÔøΩ': sb.append("&Ugrave;");break;
             case 'ÔøΩ': sb.append("&ucirc;");break;
              case 'ÔøΩ': sb.append("&Ucirc;");break;
             case 'ÔøΩ': sb.append("&uuml;");break;
             case 'ÔøΩ': sb.append("&Uuml;");break;
             case 'ÔøΩ': sb.append("&reg;");break;
             case 'ÔøΩ': sb.append("&copy;");break;
             case 'ÔøΩ': sb.append("&euro;"); break;
                */
                // be carefull with this one (non-breaking whitee space)
                case ' ':
                    sb.append("&nbsp;");
                    break;

                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

}
