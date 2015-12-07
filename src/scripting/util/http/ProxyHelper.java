/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Doan
 */
public class ProxyHelper {

    public static boolean checkProxy(String address, int port, String username, String password) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpHost proxy = new HttpHost(address, port);
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        if (Strings.isNullOrEmpty(username) == false) {
            client.getCredentialsProvider().setCredentials(new AuthScope(address, port), new UsernamePasswordCredentials(username, password));
        }
        HttpGet request = new HttpGet("http://www.google.com/");
        try {
            HttpResponse response = client.execute(request);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(ProxyHelper.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static String getRandomProxy(String proxyList) {
        String[] proxies = proxyList.split("\r\n");
        Random rnd = new Random();
        for (int i = 0; i < 5; i++) {
            try {
                int rndIndex = rnd.nextInt(proxies.length);
                String[] proxyDetails = proxies[rndIndex].split(":");
                if (proxyDetails.length == 4) {
                    if (checkProxy(proxyDetails[0], Integer.valueOf(proxyDetails[1]), proxyDetails[2], proxyDetails[3])) {
                        return proxies[rndIndex];
                    }
                } else if (proxyDetails.length == 2) {
                    if (checkProxy(proxyDetails[0], Integer.valueOf(proxyDetails[1]), null, null)) {
                        return proxies[rndIndex];
                    }
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }
}
