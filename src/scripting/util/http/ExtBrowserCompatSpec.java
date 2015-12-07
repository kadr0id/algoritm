/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.DateUtils;

/**
 *
 * @author doan
 */
public class ExtBrowserCompatSpec extends BrowserCompatSpec {

    private static final String[] DEFAULT_DATE_PATTERNS = new String[]{
        DateUtils.PATTERN_RFC1123,
        DateUtils.PATTERN_RFC1036,
        DateUtils.PATTERN_ASCTIME,
        "EEE, dd-MMM-yyyy HH:mm:ss z",
        "EEE, dd-MMM-yyyy HH-mm-ss z",
        "EEE, dd MMM yy HH:mm:ss z",
        "EEE dd-MMM-yyyy HH:mm:ss z",
        "EEE dd MMM yyyy HH:mm:ss z",
        "EEE dd-MMM-yyyy HH-mm-ss z",
        "EEE dd-MMM-yy HH:mm:ss z",
        "EEE dd MMM yy HH:mm:ss z",
        "EEE,dd-MMM-yy HH:mm:ss z",
        "EEE,dd-MMM-yyyy HH:mm:ss z",
        "EEE, dd-MM-yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss"};

    public ExtBrowserCompatSpec() {
        super(DEFAULT_DATE_PATTERNS);
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin)
            throws MalformedCookieException {
        // Oh, I am easy
        // allow all cookies
        //log.debug("custom validate");
    }
}
