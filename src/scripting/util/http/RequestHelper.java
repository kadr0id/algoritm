/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GOD
 */
public class RequestHelper {

    public static String MakeGetRequest(String url, List<NameValuePair> params, Charset encoding) {
        String paramString = URLEncodedUtils.format(params, encoding);
        return url + paramString;
    }

    public static String MakeGETRequest(String url, List<NameValuePair> params) {
        return RequestHelper.MakeGetRequest(url, params, Charset.defaultCharset());
    }

    public static String LocationUrl(HttpResponse response) {
        Header locationUrl = response.getHeaders("location")[0];
        return locationUrl.getValue();
    }

    public static String MakeAbsoluteURL(String absolute, String relative) {
        try {
            //if (absolute == "") return relative;
            URI absURI = new URI(absolute);
            //URI relURI = absURI.resolve(relative);
            URI relURI = URIUtils.resolve(absURI, relative);
            String path = relURI.getPath();
            if (relURI.getPath() == null || relURI.getPath().equals("")) {
                path = "/";
            }
            URIBuilder builder = new URIBuilder(relURI);
            relURI = builder.setPath(path).build();
            return relURI.toString();
        } catch (URISyntaxException ex) {
            Logger.getLogger(RequestHelper.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
