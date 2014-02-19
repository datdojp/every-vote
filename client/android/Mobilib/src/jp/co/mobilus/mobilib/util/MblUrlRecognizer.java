package jp.co.mobilus.mobilib.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;


/*============================================================
Test cases for URL
    (L=is a link, NL=not a link)
    HTtP://google.com --> L
    http://google.com" --> L
    http://google.com<script>alert("hello");</script> --> L
    http:// --> NL
    http://a --> L
    http://google.com&something --> L
    http://google.com</a><a>something --> L
    http://google.com05034070656 --> L

    05034070656http://google.com --> number & link
    Https://google.com.Http?id=Http --> L 
        --> when open link in browser, address bar must display: https://google.com.http?id=Http 
        --> "Https://" is lowercased by app, ".Http" is lowercased by browser, "?id=Http" is not lowercased
    href="http://google.com" --> "http://google.com"" part is display as a link
       http://google.com -> "http://google.com" part is display as a link
    a http://google.com --> "http://google.com" part is display as a link
============================================================*/
public class MblUrlRecognizer {
    private static final String HTTP_PREFIX_PATTERN = "[Hh][Tt][Tt][Pp]://";
    private static final String HTTPS_PREFIX_PATTERN = "[Hh][Tt][Tt][Pp][Ss]://";
    private static final String LINK_BODY_PATTERN = "[^\\s]+";
    private static final String HTTP_PATTERN = HTTP_PREFIX_PATTERN + LINK_BODY_PATTERN;
    private static final String HTTPS_PATTERN = HTTPS_PREFIX_PATTERN + LINK_BODY_PATTERN;

    private static final String LINK_PATTERN = "[Hh][Tt][Tt][Pp][Ss]?://" + LINK_BODY_PATTERN;
    static final Pattern p2 = Pattern.compile(LINK_PATTERN);
    StringBuilder result = new StringBuilder();
    StringBuilder source;

    public MblUrlRecognizer(String s) {
        if (s == null) s = "";
        source = new StringBuilder(s);
    }

    public String getResult() {
        while (source.length() > 0) {
            Matcher matcher = p2.matcher(source.toString());
            if (!matcher.find()) matcher = null;
            if (replaceRegex(matcher)) continue;
            result.append(replaceChars(source.toString()));

            break;
        }
        return result.toString();
    }
    private boolean replaceRegex(Matcher m) {
        if (m == null) return false;
        String hrefPrefix = "";
        String text = m.group();
        result.append(replaceChars(source.substring(0, m.start())));
        result.append("<a " + "href=\"" + replaceChars(hrefPrefix + text) + "\">" + replaceChars(text) + "</a>");
        source.delete(0, m.end());
        return true;
    }

    private String replaceChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(replaceChar(s.charAt(i)));
        }
        return sb.toString();
    }
    private String replaceChar(char c) {
        String r = "" + c;
        switch (c) {
        case '&':  r = "&amp;"; break;
        case '\"': r = "&quot;"; break;
        case '<':  r = "&lt;"; break;
        case '>':  r = "&gt;"; break;
        case ' ':  r = "&nbsp;"; break;
        case '\n': r = "<br>"; break;
        default: break;
        }
        return r;
    }

    public static boolean isLink(String s) {
        return !TextUtils.isEmpty(s) && s.matches(LINK_PATTERN);
    }

    // android do not understand prefixes like "HTtP" or "hTtP"
    // so we need to make all http/https prefixes lower-case
    public static String lowerCaseHttpxPrefix(String link) {
        if (TextUtils.isEmpty(link)) return link;
        String ret;
        if (link.matches(HTTP_PATTERN)) {
            ret = link.replaceFirst(HTTP_PREFIX_PATTERN, "http://");
        } else if (link.matches(HTTPS_PATTERN)) {
            ret = link.replaceFirst(HTTPS_PREFIX_PATTERN, "https://");
        } else {
            ret = link;
        }
        return ret;
    }
}
