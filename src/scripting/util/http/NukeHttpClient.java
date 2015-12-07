/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import com.google.common.base.Strings;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.VirtualFileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.util.TextUtility;
import scripting.util.concurrent.ManualResetEvent;
import scripting.util.diff_match_patch;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author GOD
 */
public class NukeHttpClient extends DefaultHttpClient {

    private String rawBody;
    private org.jsoup.nodes.Document body;
    private Charset charset;
    private HttpRequestBase currentReq;
    private HashMap<String, String> reqParams;
    private HttpContext localContext;
    private HashMap<String, String> httpParams;
    private HttpContext context;
    private String currentUrl = "";
    private Boolean isStopped = false;
    private int timeout = 0;
    private ManualResetEvent responseCompletedEvent;
    private HashMap<String, String> headers;
    private String defaultUserAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    private ExecutorService service;
    private final diff_match_patch matcher;
    private HashMap<String, String> currentResponseHeaders = new HashMap<String, String>();
    private Proxy currentProxy;
    private Header[] responseHeaders;

    public NukeHttpClient() {
        super();
        matcher = new diff_match_patch();
        matcher.Match_Threshold = (float) 0.4;

        reqParams = new HashMap<String, String>();
        //httpParams = new LinkedList<NameValuePair>();
        httpParams = new LinkedHashMap<String, String>();
        context = new BasicHttpContext();
        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
    }

    public NukeHttpClient(boolean SSL) throws NoSuchAlgorithmException, KeyManagementException {
        super();
        matcher = new diff_match_patch();
        matcher.Match_Threshold = (float) 0.4;

        //this.service = Executors.newSingleThreadExecutor();
        SSLContext ctx = SSLContext.getInstance("TLS");

        X509TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        X509HostnameVerifier verifier = new X509HostnameVerifier() {
            @Override
            public void verify(String string, SSLSocket ssls) throws IOException {
            }

            @Override
            public void verify(String string, X509Certificate xc) throws SSLException {
            }

            @Override
            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        };
        reqParams = new HashMap<String, String>();
        //httpParams = new LinkedList<NameValuePair>();
        httpParams = new LinkedHashMap<String, String>();
        context = new BasicHttpContext();
        ctx.init(null, new TrustManager[]{tm}, null);
        SSLSocketFactory ssf = new SSLSocketFactory(ctx);
        ssf.setHostnameVerifier(verifier);
        ClientConnectionManager ccm = this.getConnectionManager();
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme("https", ssf, 443));
        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        //this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);

        CookieSpecFactory csf = new CookieSpecFactory() {
            public CookieSpec newInstance(HttpParams params) {
                return new ExtBrowserCompatSpec();
            }
        };
        this.getCookieSpecs().register("easy", csf);
        this.getParams().setParameter(
                ClientPNames.COOKIE_POLICY, "easy");
        init();
    }

    public void disableProxy() {
        this.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
    }

    public void enableProxy() {
        if (currentProxy != null) {
            this.setProxy(currentProxy);
        }
    }

    public void setProxy(Proxy p) {
        this.currentProxy = p;
        HttpHost proxy = new HttpHost(p.getAddress(), p.getPort());
        this.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        if (Strings.isNullOrEmpty(p.getUsername()) == false) {
            this.getCredentialsProvider().setCredentials(new AuthScope(p.getAddress(), p.getPort()), new UsernamePasswordCredentials(p.getUsername(), p.getPassword()));
        }
    }

    public Proxy getProxy() {
        return currentProxy;
    }

    public void setRequestExecutorService(ExecutorService service) {
        this.service = service;
    }

    public ExecutorService getRequestExecutorService() {
        return this.service;
    }

    public void setDefaulUserAgent(String defauUserAgent) {
        this.defaultUserAgent = defauUserAgent;
    }

    public void setTimeOut(int timeout) {
        this.timeout = timeout;
        HttpParams params = this.getParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout * 1000);
        HttpConnectionParams.setSoTimeout(params, timeout * 1000);

    }

    public void init() {
        this.initInterceptor();
        this.initRedirectStrategy();
    }

    public void initRedirectStrategy() {
        this.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
        this.setRedirectStrategy(new NukeRedirectStrategy());
    }

    public void setStopEvent(ManualResetEvent event) {
        this.responseCompletedEvent = event;
    }

    public ManualResetEvent getStopEvent() {
        return this.responseCompletedEvent;
    }

    public void initInterceptor() {
        this.headers = new HashMap<String, String>();
        this.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                Iterator it = NukeHttpClient.this.headers.entrySet().iterator();
//                request.removeHeaders("Connection");
//                request.removeHeaders("Proxy-Connection");
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    request.addHeader((String) pairs.getKey(), (String) pairs.getValue());
                }
            }
        });
    }

//    public void setProxy (scraper.data.entity.Proxy p) {
//        HttpHost proxy = new HttpHost(p.getAddress(), p.getPort());
//        this.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//        if (Strings.isNullOrEmpty(p.getUsername()) == false) {
//            this.getCredentialsProvider().setCredentials(new AuthScope(p.getAddress(), p.getPort()), new UsernamePasswordCredentials(p.getUsername(), p.getPassword()));
//        }
//
//    }
    public int AddRequestHeader(String key, String value) {
        if (key.equals("User-Agent")) {
            this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, value);
        } else {
            //reqParams.put(key,value);
            this.headers.put(key, value);
        }
        return 1;
    }

    public int RemoveRequestHeader(String key) {
        if (key.equals("User-Agent")) {
            this.getParams().removeParameter(CoreProtocolPNames.USER_AGENT);
        } else {
            this.headers.remove(key);
        }
        return 1;
    }

    public void ClearCookies() {
        this.getCookieStore().clear();
    }

    public void ClearClientHeaders() {
        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, defaultUserAgent);
        this.headers.clear();
    }

    public void AddHiddenParamsContains(String innerText) {
        Element myForm = this.FindElement("form", innerText);
        if (myForm != null) {
            Elements hiddenInputs = myForm.select("input[type=hidden]");
            for (Iterator<Element> i = hiddenInputs.iterator(); i.hasNext();) {
                Element e = i.next();
                if (e.attr("value") != null) {
                    AddPostParam(e.attr("name"), e.attr("value"));
                    // httpParams.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
                } else {
                    AddPostParam(e.attr("name"), "");
                    //httpParams.add(new BasicNameValuePair(e.attr("name"), ""));
                }
            }
        }
    }

    public void AddFixedPostParamForm(Element form) {


        if ((form != null)) {
            Elements elements;
            //Input
            elements = form.select("input");
            if (elements != null) {

                for (int i = 0; i < elements.size(); i++) {
                    Element e = elements.get(i);
                    if (e.hasAttr("name") && !Strings.isNullOrEmpty(e.attr("name"))) {
                        if (e.hasAttr("value")) {
                            AddPostParam(e.attr("name"), e.attr("value"));
                            //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), elements.get(i).attr("value")));
                        } else {
                            AddPostParam(e.attr("name"), "");
                            //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), ""));
                        }
                    }
                }
            }

            //select tags
            elements = form.select("select");

            if (elements != null) {

                Elements options = null;
                Elements selectedOptionNodes = null;
                Iterator<Element> itSelect = elements.iterator();
                while (itSelect.hasNext()) {
                    Element selectNode = itSelect.next();
                    if (selectNode.hasAttr("name")) {

                        selectedOptionNodes = selectNode.select("option[selected]");
                        if (selectedOptionNodes.size() > 0 && selectedOptionNodes.last().hasAttr("value") && !Strings.isNullOrEmpty(selectedOptionNodes.last().attr("value")) && !selectedOptionNodes.last().attr("value").toString().equalsIgnoreCase("-1")) {
                            AddPostParam(selectNode.attr("name"), selectedOptionNodes.last().attr("value"));
                        } else {

                            options = selectNode.select("option[value]");
                            if (options == null || options.size() == 0) {// Select tags does not have Option with value attribut
                                options = selectNode.select("option");
                                if (options != null) {

                                    if (options.size() > 1) {
                                        int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
                                        AddPostParam(selectNode.attr("name"), options.get(indexRandom).text());
                                    } else if (options.size() > 0) {
                                        AddPostParam(selectNode.attr("name"), options.last().text());
                                    }
                                }

                            } else {//option has value


                                if (options.size() > 1) {
                                    int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
                                    AddPostParam(selectNode.attr("name"), options.get(indexRandom).attr("value"));
                                } else if (options.size() > 0) {
                                    AddPostParam(selectNode.attr("name"), options.last().attr("value"));
                                }


                            }
                        }
                    }
                }
            }
        }
    }

    
    public void AddFixedPostParamFormValue(Element form) {


        if ((form != null)) {
            Elements elements;
            //Input
            elements = form.select("input");
            if (elements != null) {

                for (int i = 0; i < elements.size(); i++) {
                    Element e = elements.get(i);
                    if (e.hasAttr("name") && !Strings.isNullOrEmpty(e.attr("name"))) {
                        if (e.hasAttr("value")) {
                            if (Strings.isNullOrEmpty(e.attr("value"))){
                                AddPostParam(e.attr("name"), TextUtility.RandomFName());
                            }else{
                                AddPostParam(e.attr("name"), e.attr("value"));
                            }                            
                        } else {
                            AddPostParam(e.attr("name"), TextUtility.RandomFName());
                            //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), ""));
                        }
                    }
                }
            }

            //select tags
            elements = form.select("select");

            if (elements != null) {

                Elements options = null;
                Elements selectedOptionNodes = null;
                Iterator<Element> itSelect = elements.iterator();
                while (itSelect.hasNext()) {
                    Element selectNode = itSelect.next();
                    if (selectNode.hasAttr("name")) {

                        selectedOptionNodes = selectNode.select("option[selected]");
                        if (selectedOptionNodes.size() > 0 && selectedOptionNodes.last().hasAttr("value") && !Strings.isNullOrEmpty(selectedOptionNodes.last().attr("value")) && !selectedOptionNodes.last().attr("value").toString().equalsIgnoreCase("-1")) {
                            AddPostParam(selectNode.attr("name"), selectedOptionNodes.last().attr("value"));
                        } else {

                            options = selectNode.select("option[value]");
                            if (options == null || options.size() == 0) {// Select tags does not have Option with value attribut
                                options = selectNode.select("option");
                                if (options != null) {

                                    if (options.size() > 1) {
                                        int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
                                        AddPostParam(selectNode.attr("name"), options.get(indexRandom).text());
                                    } else if (options.size() > 0) {
                                        AddPostParam(selectNode.attr("name"), options.last().text());
                                    }
                                }

                            } else {//option has value


                                if (options.size() > 1) {
                                    int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
                                    AddPostParam(selectNode.attr("name"), options.get(indexRandom).attr("value"));
                                } else if (options.size() > 0) {
                                    AddPostParam(selectNode.attr("name"), options.last().attr("value"));
                                }


                            }
                        }
                    }
                }
            }
        }
    }


    public void AddFixedPostParam(String outerHtml) {
        Elements elements;
        Element myForm;
        if (outerHtml.startsWith("@@@")) {
            String[] tmp = outerHtml.split("@@@");
            myForm = this.FindElement(tmp[1]);
        } else {
            myForm = this.FindElement("form", outerHtml);
        }

        if ((myForm != null)) {

            //Input
            elements = myForm.select("input");
            if (elements != null) {

                for (int i = 0; i < elements.size(); i++) {
                    Element e = elements.get(i);
                    if (e.hasAttr("name")) {
                        if (e.hasAttr("value")) {
                            AddPostParam(e.attr("name"), e.attr("value"));
                            //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), elements.get(i).attr("value")));
                        } else {
                            AddPostParam(e.attr("name"), "");
                            //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), ""));
                        }
                    }
                }
            }
            //select tags
            elements = myForm.select("select");

            if (elements != null) {


                Elements options = null;

                Iterator<Element> itSelect = elements.iterator();
                while (itSelect.hasNext()) {
                    Element selectNode = itSelect.next();
                    if (!selectNode.hasAttr("name")) {
                        break;
                    }
                    options = selectNode.select("option[value]");
                    if (options == null) {
                        break;
                    }
                    boolean isAdded = false;
                    Iterator<Element> itOption = options.iterator();
                    while (itOption.hasNext()) {
                        Element optionNode = itOption.next();
                        if (optionNode.hasAttr("selected") && optionNode.hasAttr("value")) {
                            AddPostParam(selectNode.attr("name"), optionNode.attr("value"));
                            isAdded = true;
                            break;
                            //httpParams.add(new BasicNameValuePair(selectNode.attr("name"), optionNode.attr("value")));
                        }
                    }
                    if (isAdded) {
                        break;
                    }
                    if (options.size() > 1) {
                        int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
                        AddPostParam(selectNode.attr("name"), options.get(indexRandom).attr("value"));
                    } else if (options.size() > 0) {
                        AddPostParam(selectNode.attr("name"), options.last().attr("value"));
                    }
                }
            }
        }
    }

    public void AddHiddenParams(String form) {
        //Elements myForms = body.select("form#" + formname);
        Elements myForms = body.select(form);
        if (myForms.isEmpty() == false) {
            Element myForm = myForms.first();
            Elements hiddenInputs = myForm.select("input[type=hidden]");
            for (Iterator<Element> i = hiddenInputs.iterator(); i.hasNext();) {
                Element e = i.next();
                if (e.attr("value") != null) {
                    httpParams.put(e.attr("name"), e.attr("value"));
                } else {
                    httpParams.put(e.attr("name"), "");
                }
            }
        }
    }

    public void AddPostParam(String name, String value) {
        httpParams.put(name, value);
    }

    public String GetPostParam(String name) {
        return httpParams.get(name);
    }

    public String GetResponseHeader(String name) {
        if (this.responseHeaders != null && this.responseHeaders.length > 0) {
            for (Header head : this.responseHeaders) {
                if (head.getName().equals(name)) {
                    return head.getValue();
                }
            }
        }
        return "";
    }

    public void RemovePostParam(String name) {
        httpParams.remove(name);
    }

    public void ClearPostParams() {
        httpParams.clear();
        fileParams.clear();
    }

    public void ClearReqParams() {
        reqParams.clear();
    }

    public void ClearAll() {
        this.ClearPostParams();
        this.ClearClientHeaders();
        this.getCookieStore().clear();
        //this.
    }

    public void SetCookie(String name, String value) throws URISyntaxException {
        this.SetCookie(currentUrl, name, value);
    }

    public String getCurrentURL() {
        return currentUrl;
    }

    public void SetCookie(String url, String name, String value) throws URISyntaxException {
        URI uri = new URI(RequestHelper.MakeAbsoluteURL(currentUrl, url));
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(uri.getHost());
        this.getCookieStore().addCookie(cookie);
    }

    public String GetCookie(String url, String name) throws URISyntaxException {
        URI uri = new URI(url);
        for (Cookie c : this.getCookieStore().getCookies()) {
            //if (c.getDomain().equalsIgnoreCase(uri.getHost())) {
            if ((uri.getHost().toLowerCase().contains(c.getDomain().toLowerCase()))) {
                if (c.getName().equals(name)) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    public String GetAllCookieString(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String hostName = uri.getHost();
        int port = uri.getPort();
        if (port < 0) {

            // Target port will be selected by the proxy.
            // Use conventional ports for known schemes
            String scheme = uri.getScheme();
            if (scheme.equalsIgnoreCase("http")) {
                port = 80;
            } else if (scheme.equalsIgnoreCase("https")) {
                port = 443;
            } else {
                port = 0;
            }

        }

        String path = uri.getPath();
        boolean isSecure = (port == 80) ? false : true;

        CookieOrigin cookieOrigin = new CookieOrigin(
                hostName,
                port,
                path,
                isSecure);

        String policy = (String) (this.getParams().getParameter(ClientPNames.COOKIE_POLICY));

        // Get an instance of the selected cookie policy
        CookieSpec cookieSpec = this.getCookieSpecs().getCookieSpec(policy);
        // Get all cookies available in the HTTP state
        List<Cookie> cookies = new ArrayList<Cookie>(this.getCookieStore().getCookies());
        // Find cookies matching the given origin
        List<Cookie> matchedCookies = new ArrayList<Cookie>();

        DateTime n = new DateTime();
        //n.minusMillis(0);
        Date now = n.toDate();

        for (Cookie cookie : cookies) {
            if (!cookie.isExpired(now)) {
                if (cookieSpec.match(cookie, cookieOrigin)) {

                    matchedCookies.add(cookie);
                }
            } else {
            }
        }
        // Generate Cookie request headers
        String allCookies = "";
        if (!matchedCookies.isEmpty()) {
            List<Header> headers = cookieSpec.formatCookies(matchedCookies);
            for (Header header : headers) {
                allCookies = header.getValue();
            }
        }
        return allCookies;
    }

    public String getRawBody() {
        return rawBody;
    }

    public org.jsoup.nodes.Document getBody() {
        return body;
    }

    public void setBody(Document doc) {
        this.body = doc;
    }

    public void InternalRedirect(String url, int nRedirect) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpGet httpget = new HttpGet(url);
        try {
            if (nRedirect >= 11) {
                return;
            }
            currentReq = httpget;
            final HttpContext localContext = new BasicHttpContext();

            Future f = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(httpget, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.WARNING, null, ex);
                    } finally {
                        httpget.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);

            if (waitResult == false) {
                httpget.abort();
                try {
                    f.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout! at " + url);
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                httpget.abort();
                try {
                    f.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);

//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    this.headers.remove("Referer");
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    InternalRedirect(metaURL, nRedirect + 1);
                }
            }
        } finally {
            httpget.releaseConnection();
            responseCompletedEvent.reset();

        }
    }

    protected boolean checkNoScript(Elements meta) {
        Elements parents = meta.parents();
        Iterator<Element> it = parents.iterator();
        boolean result = true;
        while (it.hasNext()) {
            Element parent = it.next();
            if (parent.tagName().equalsIgnoreCase("noscript")) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void Navigate(String url, boolean autoRedirect) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpGet httpget = new HttpGet(url);
        try {
            currentReq = httpget;

            final HttpContext localContext = new BasicHttpContext();

            Future f = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(httpget, localContext);
                        HttpEntity entity = response.getEntity();
//                        System.out.println("\nnavigate to:    " + httpget.getURI().toString());
                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }
                        responseHeaders = response.getAllHeaders();

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.WARNING, null, ex);
                        if (ex instanceof ConnectException) {
                        }
                    } finally {
                        httpget.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(500, TimeUnit.SECONDS);
            //responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    f.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout! at " + url);
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    f.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
//            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (currentReq.getURI().isAbsolute()) {
//                currentUrl = currentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            if (autoRedirect) {
                Elements meta = body.select("html head meta[http-equiv]");
                if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                    if (checkNoScript(meta)) {
                        String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                        String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                        String metaURL = match == null ? null : match[2];
                        //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                        this.headers.remove("Referer");
                        InternalRedirect(metaURL, 1);
                    }
                }
            }
        } finally {
            httpget.releaseConnection();
            responseCompletedEvent.reset();

        }


    }

    public void Navigate(String url) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        this.Navigate(url.trim(), true);
    }

    public String DownloadString(String url) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException {
        String result = null;
        final HttpGet httpget = new HttpGet(url);
        try {
            Future task = service.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String result = null;
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(httpget, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            result = EntityUtils.toString(entity, charset);
                        } else {
                            result = EntityUtils.toString(entity);
                        }

                    } catch (IOException ex) {
                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        httpget.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                        return result;
                    }
                }
            });
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            result = (String) task.get();
        } finally {
            httpget.releaseConnection();
            responseCompletedEvent.reset();
            return result;
        }

    }

    public InputStream DownloadGoogleCaptcha(String key) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException {
        String keyInfoStr = this.DownloadString("http://api.recaptcha.net/noscript?k=" + key);
        return null;
    }

    public byte[] DownloadImageInByte(String url) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpGet httpget = new HttpGet(url);
        currentReq = httpget;
        byte[] result = null;
        try {

            Future task = service.submit(new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    byte[] result = null;
                    try {

                        HttpResponse response = NukeHttpClient.this.execute(httpget);
                        HttpEntity entity = response.getEntity();
                        //byte[] bytes = EntityUtils.toByteArray(entity);

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();


                        result = EntityUtils.toByteArray(entity);


                    } catch (IOException ex) {
                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        httpget.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                        return result;
                    }
                }
            });


            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            byte[] bytes = (byte[]) task.get();
            result = bytes;
        } finally {
            httpget.releaseConnection();
            responseCompletedEvent.reset();
            return result;
        }
    }

    public int CountImageInByte(String url) throws InterruptedException, TaskStoppedException, ExecutionException, SubmissionTimeoutException {
        int ireturn = 0;
        try {
            byte[] img = DownloadImageInByte(url);
            ireturn = img.length;
        } catch (Exception ex) {
            ireturn = -1;
        }

        return ireturn;
    }

    public InputStream DownloadImage(String url) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpGet httpget = new HttpGet(url);
        currentReq = httpget;
        InputStream result = null;
        try {

            Future task = service.submit(new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    byte[] result = null;
                    try {

                        HttpResponse response = NukeHttpClient.this.execute(httpget);
                        HttpEntity entity = response.getEntity();
                        //byte[] bytes = EntityUtils.toByteArray(entity);

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();


                        result = EntityUtils.toByteArray(entity);


                    } catch (IOException ex) {
                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        httpget.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                        return result;
                    }
                }
            });


            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            byte[] bytes = (byte[]) task.get();
            result = new ByteArrayInputStream(bytes);
        } finally {
            httpget.releaseConnection();
            responseCompletedEvent.reset();
            return result;
        }
    }
    private List<NameValuePair> fileParams = new LinkedList<NameValuePair>();

    public void AddFilePostParam(String param, String fileName) {
        this.fileParams.add(new BasicNameValuePair(param, fileName));
    }

    private HashMap<String, byte[]> virtualFileParams = new HashMap<String, byte[]>();
    public void AddVirtualFilePostParam(String param, byte[] data) {
        this.virtualFileParams.put(param, data);
    }


    public void MultiPost(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpPost postReq = new HttpPost(url);
        try {
            currentReq = postReq;
            MultipartEntity data = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            //MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
            for (NameValuePair filePart : fileParams) {

                if (Strings.isNullOrEmpty(filePart.getValue())) {
                    data.addPart(filePart.getName(), new AbstractContentBody("application/octet-stream") {
                        @Override
                        public String getFilename() {
                            return "";
                        }

                        @Override
                        public void writeTo(OutputStream out) throws IOException {
                            out.flush();
                        }

                        @Override
                        public String getCharset() {
                            return null;
                        }

                        @Override
                        public String getTransferEncoding() {
                            return MIME.ENC_BINARY;
                        }

                        @Override
                        public long getContentLength() {
                            return 0;
                        }
                    });
                } else {
                    File f = new File(filePart.getValue());
                    Collection s = MimeUtil.getMimeTypes(f);
                    if (s != null) {
                        data.addPart(filePart.getName(), new FileBody(f, s.toString()));
                    } else {
                        data.addPart(filePart.getName(), new FileBody(f));
                    }
                }
            }

            Iterator<Entry<String, byte[]>> virtualFileIT = virtualFileParams.entrySet().iterator();
            while (virtualFileIT.hasNext()) {
                Entry<String, byte[]> param = virtualFileIT.next();
                String name = param.getKey();
                byte[] value = param.getValue();
                data.addPart(name, new VirtualFileBody(value));
            }

            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> param = it.next();
                String name = param.getKey();
                String value = param.getValue();
                String[] values = value.split("\\|x\\|");
                for (int i = 0; i < values.length; i++) {
                    data.addPart(name, new StringBody(values[i]));
                }
            }




            //UrlEncodedFormEntity data = new UrlEncodedFormEntity(httpParams);
            this.ClearPostParams();
            postReq.setEntity(data);

            final HttpContext localContext = new BasicHttpContext();
            Future g = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }

            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void MultiPostSlideshare(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        final HttpPost postReq = new HttpPost(url);
        try {
            currentReq = postReq;
            MultipartEntity data = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            data.addPart("Filename", new StringBody(httpParams.get("Filename")));
            data.addPart("key", new StringBody(httpParams.get("key")));
            data.addPart("signature", new StringBody(httpParams.get("signature")));
            data.addPart("AWSAccessKeyId", new StringBody(httpParams.get("AWSAccessKeyId")));
            data.addPart("acl", new StringBody(httpParams.get("acl")));
            data.addPart("success_action_status", new StringBody(httpParams.get("success_action_status")));
            data.addPart("file_id", new StringBody(httpParams.get("file_id")));
            data.addPart("policy", new StringBody(httpParams.get("policy")));

            //MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
            for (NameValuePair filePart : fileParams) {
                File f = new File(filePart.getValue());
                Collection s = MimeUtil.getMimeTypes(f);
                if (s != null) {
                    data.addPart(filePart.getName(), new FileBody(f, s.toString()));
                } else {
                    data.addPart(filePart.getName(), new FileBody(f));
                }
            }

            data.addPart("Upload", new StringBody(httpParams.get("Upload")));


            //UrlEncodedFormEntity data = new UrlEncodedFormEntity(httpParams);
            this.ClearPostParams();
            postReq.setEntity(data);

            final HttpContext localContext = new BasicHttpContext();
            Future g = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }

            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void PATCHRaw(String url, String data) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
        final HttpPatch postReq = new HttpPatch(url);
        try {
            currentReq = postReq;
            StringEntity entity = new StringEntity(data);
            this.ClearPostParams();
            postReq.setEntity(entity);
            final HttpContext localContext = new BasicHttpContext();
            Future task = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (localCurrentReq.getURI().isAbsolute()) {
                currentUrl = localCurrentReq.getURI().toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
            }
            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void POSTRaw(String url, String data) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
        final HttpPost postReq = new HttpPost(url);
        try {
            currentReq = postReq;
            StringEntity entity = new StringEntity(data);
            this.ClearPostParams();
            postReq.setEntity(entity);
            final HttpContext localContext = new BasicHttpContext();
            Future task = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();

                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (localCurrentReq.getURI().isAbsolute()) {
                currentUrl = localCurrentReq.getURI().toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
            }
            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public int GET(String url, List<NameValuePair> params) {
        url = RequestHelper.MakeGETRequest(url, params);
        try {
            HttpGet httpget = new HttpGet(url);
            currentReq = httpget;
            HttpResponse response = this.execute(httpget);
            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                rawBody = EntityUtils.toString(entity, charset);
            } else {
                rawBody = EntityUtils.toString(entity);
            }

            body = Jsoup.parse(rawBody);

            return 1;
        } catch (Exception ex) {
            //log exepction here
            return 0;
        }
    }

    public void POST(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
        final HttpPost postReq = new HttpPost(url);
        //  final HttpPatch postReq1 = new HttpPatch(url);

        try {
            currentReq = postReq;
            List<NameValuePair> paramList = new LinkedList<NameValuePair>();
            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> param = it.next();
                String name = param.getKey().replaceAll("::xx::yy::[0-9]*", "");
                String value = param.getValue();
                String[] values = value.split("\\|x\\|");
                for (int i = 0; i < values.length; i++) {
                    paramList.add(new BasicNameValuePair(name, values[i]));
                }
            }

            UrlEncodedFormEntity data = new UrlEncodedFormEntity(paramList, "UTF-8");
            this.ClearPostParams();
            postReq.setEntity(data);
            final HttpContext localContext = new BasicHttpContext();
            Future task = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();
//                        System.out.println("\nPosted to:" + postReq.getURI().toString());
                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity, Charset.forName("UTF-8"));
                        }
                        responseHeaders = response.getAllHeaders();
                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void Patch(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
        //final HttpPost postReq = new HttpPost(url);
        final HttpPatch postReq = new HttpPatch(url);

        try {
            currentReq = postReq;
            List<NameValuePair> paramList = new LinkedList<NameValuePair>();
            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> param = it.next();
                String name = param.getKey().replaceAll("::xx::yy::[0-9]*", "");
                String value = param.getValue();
                String[] values = value.split("\\|x\\|");
                for (int i = 0; i < values.length; i++) {
                    paramList.add(new BasicNameValuePair(name, values[i]));
                }
            }

            UrlEncodedFormEntity data = new UrlEncodedFormEntity(paramList, "UTF-8");
            this.ClearPostParams();
            postReq.setEntity(data);
            final HttpContext localContext = new BasicHttpContext();
            Future task = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();
//                        System.out.println("\nPosted to:" + postReq.getURI().toString());
                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity, Charset.forName("UTF-8"));
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void PUT(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
        final HttpPut postReq = new HttpPut(url);
        try {
            currentReq = postReq;
            List<NameValuePair> paramList = new LinkedList<NameValuePair>();
            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> param = it.next();
                String name = param.getKey().replaceAll("::xx::yy::[0-9]*", "");
                String value = param.getValue();
                String[] values = value.split("\\|x\\|");
                for (int i = 0; i < values.length; i++) {
                    paramList.add(new BasicNameValuePair(name, values[i]));
                }
            }

            UrlEncodedFormEntity data = new UrlEncodedFormEntity(paramList, "UTF-8");
            this.ClearPostParams();
            postReq.setEntity(data);
            final HttpContext localContext = new BasicHttpContext();
            Future task = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpResponse response;
                        response = NukeHttpClient.this.execute(postReq, localContext);
                        HttpEntity entity = response.getEntity();
//                        System.out.println("\nPosted to:" + postReq.getURI().toString());
                        ContentType contentType = ContentType.getOrDefault(entity);
                        Charset charset = contentType.getCharset();

                        if (charset != null) {
                            rawBody = EntityUtils.toString(entity, charset);
                        } else {
                            rawBody = EntityUtils.toString(entity, Charset.forName("UTF-8"));
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        postReq.releaseConnection();
                        responseCompletedEvent.setSource("completed");
                        responseCompletedEvent.set();
                    }

                }
            });

            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
            responseCompletedEvent.reset();
            if (waitResult == false) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                try {
                    task.get();
                } catch (Exception ex) {
                }
                throw new TaskStoppedException("Task stopped");
            }
            responseCompletedEvent.reset();
            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
            if (tmpURI.isAbsolute()) {
                currentUrl = tmpURI.toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
            }

            Elements meta = body.select("html head meta[http-equiv]");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                if (checkNoScript(meta)) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public boolean responseContains(String text) {
        String[] textEls = text.split("\\|");
        for (int i = 0; i < textEls.length; i++) {
            if (textEls[i].indexOf("*") == 0) {
                String[] txt = textEls[i].split("\\*");
                if (txt.length == 0) {
                    //return false;
                }
                if (txt[1].startsWith("@")) {
                    String[] innerTxt = txt[1].split("@");
                    if (innerTxt.length == 0) {
                        //return false;
                    } else {
                        Pattern p = Pattern.compile(innerTxt[1], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                        Matcher m = p.matcher(this.getCurrentURL());
                        if (m.find()) {
                            return true;
                        }
                        //else return false;
                    }
                } else {
                    if (this.getCurrentURL().contains(txt[1])) {
                        return true;
                    }
                }
            } else if (rawBody.contains(textEls[i])) {
                return true;
            }
        }
        return false;
    }

    public Element FindItem(String selector, boolean forward) {
        Elements els = body.select(selector);
        if (els.isEmpty()) {
            return null;
        } else {
            if (forward == true) {
                return els.first();
            } else {
                return els.last();
            }
        }
    }

    public Elements FindElements(String selector) {
        Elements els = body.select(selector);
        return els;
    }

    public Element FindElement(String selector) {
        return FindItem(selector, false);
    }

    public Element FindClosestElement(Element e, String tag, String innerText) {
        Element p = e;
        while (p != null) {
            p = p.parent();
            Element c = FindElement(tag, innerText, p, true);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public Element FindElement(String tag, String innerText) {
        return this.FindElement(tag, innerText, false);
    }

    public List<Element> FindElements(String tag, String innerText) {
        List<Element> result = new LinkedList<Element>();
        Elements es = body.select(tag);
        String lInnerText = innerText.toLowerCase();
        int s = es.size();

        for (int i = 0; i < s; i++) {
            Element e = es.get(i);
            if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
                result.add(e);
            }
        }
        return result;
//        if (result.size() == 0) {
//            return null;
//        } else {
//            return result;
//        }

    }

    public Element FindElement(String tag, String innerText, boolean first) {
        return this.FindElement(tag, innerText, body, first);
    }

    public Element FindElement(String selector, Element parent, boolean first) {
        Elements els = parent.select(selector);
        if (els.isEmpty()) {
            return null;
        } else {
            if (first == true) {
                return els.first();
            } else {
                return els.last();
            }
        }

    }

    public Element FindElement(String tag, String innerText, Element parent, boolean first) {
        Elements es = parent.select(tag);
        String lInnerText = innerText.toLowerCase();
        int s = es.size();
        if (innerText.startsWith("@@@")) {
            String[] tmp = innerText.split("@@@");
            if (tmp.length == 0) {
                return null;
            }
            innerText = tmp[1];
            Pattern p = Pattern.compile(innerText, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (first == true) {
                for (int i = 0; i < s; i++) {
                    Element e = es.get(i);
                    Matcher m = p.matcher(e.outerHtml().toLowerCase());
                    if (m.find()) {
                        return e;
                    }
                }
            } else {
                for (int i = s - 1; i >= 0; i--) {
                    Element e = es.get(i);
                    Matcher m = p.matcher(e.outerHtml().toLowerCase());
                    if (m.find()) {
                        return e;
                    }
                }
            }
        } else {
            if (first == true) {
                for (int i = 0; i < s; i++) {
                    Element e = es.get(i);
                    if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
                        return e;
                    }
                }
            } else {
                for (int i = s - 1; i >= 0; i--) {
                    Element e = es.get(i);
                    if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    public Element FindElement(Element parents, String selector) {
        return this.FindElement(parents, selector, true);
    }

    public Element FindElement(Element parents, String selector, boolean first) {
        Elements result = parents.select(selector);
        if (result.isEmpty()) {
            return null;
        } else {
            if (first == true) {
                return result.first();
            } else {
                return result.last();
            }
        }
    }

    public void stop() {
        synchronized (this.isStopped) {
            this.isStopped = true;
            if (currentReq != null) {
                currentReq.abort();
            }
        }
    }

    public void shutdown() {
        //this.service.shutdown();
        //this.service = null;
        this.getConnectionManager().shutdown();
    }

    private String FindUrlByTitleFuzzy(String title) throws Exception {
//        String shortTitle = TextUtility.Left(title, title.length() / 2);
//        Element el = FindElement("a", title, true);
//        return el.attr("href");
        int length = title.length();
        int left = 0;
        int right = length - 1;
        left = (right + left) / 2;
        int chosenLength = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String shortTitle = TextUtility.Left(title, mid + 1);
            List<Element> es = FindElementsFuzzy("a", shortTitle);
            if (es.size() > 0) {
                if (mid > chosenLength) {
                    chosenLength = mid + 1;
                }
            }
            if (es.size() > 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        if (chosenLength > 0) {
            Element el = FindElementFuzzy("a", TextUtility.Left(title, chosenLength));
            if (el != null) {
                return el.attr("href");
            } else {
                return "";
                //throw new Exception("No Link found!");
            }
        } else {

            return "";

        }
    }

    public String FindUrlByTitle(String title) throws Exception {
        if (FindElement("a", title, true) != null) {
            return FindElement("a", title, true).attr("href");
        }
        int length = title.length();
        int left = 0;
        int right = length - 1;
        left = left + (right - left) / 2;
        int chosenLength = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String shortTitle = TextUtility.Left(title, mid + 1);
            List<Element> es = FindElements("a", shortTitle);
            if (es.size() > 0) {
                if (mid > chosenLength) {
                    chosenLength = mid + 1;
                }
            }
            if (es.size() > 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        if (chosenLength > 0) {
            Element el = FindElement("a", TextUtility.Left(title, chosenLength), true);
            if (el != null) {
                return el.attr("href");
            } else {
                return FindUrlByTitleFuzzy(title);
                //throw new Exception("No Link found!");
            }
        } else {
            return FindUrlByTitleFuzzy(title);
        }
    }

    public Element FindElementFuzzy(String tag, String innerText) {
        return this.FindElementFuzzy(tag, innerText, false);
    }

    public List<Element> FindElementsFuzzy(String tag, String innerText) {
        List<Element> result = new LinkedList<Element>();
        Elements es = body.select(tag);
        String lInnerText = innerText.toLowerCase();
        lInnerText = TextUtility.Left(lInnerText, 32);
        int s = es.size();

        for (int i = 0; i < s; i++) {
            Element e = es.get(i);
//            if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                result.add(e);
//            }
            String html = e.outerHtml().toLowerCase();
            int location = matcher.match_main(html, lInnerText, 0);
            if (location != -1) {
                result.add(e);
            }
        }
        return result;
//        if (result.size() == 0) {
//            return null;
//        } else {
//            return result;
//        }

    }

    public Element FindElementFuzzy(String tag, String innerText, boolean first) {
        return this.FindElementFuzzy(tag, innerText, body, first);
    }

    private Element FindElementFuzzy(String tag, String innerText, Element parent, boolean first) {
        Elements es = parent.select(tag);
        String lInnerText = innerText.toLowerCase();
        lInnerText = TextUtility.Left(lInnerText, 32);
        int s = es.size();
        if (first == true) {
            for (int i = 0; i < s; i++) {
                Element e = es.get(i);
                String html = e.outerHtml().toLowerCase();
//                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                    return e;
//                }
                int location = matcher.match_main(html, lInnerText, 0);
                if (location != -1) {
                    return e;
                }
            }
        } else {
            for (int i = s - 1; i >= 0; i--) {
                Element e = es.get(i);
                String html = e.outerHtml().toLowerCase();
//                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                    return e;
//                }
                int location = matcher.match_main(html, lInnerText, 0);
                if (location != -1) {
                    return e;
                }
            }
        }
        return null;
    }
}
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package seo.utilities.http;
//
//import org.jsoup.nodes.Document;
//import seo.util.http.RequestHelper;
//import eu.medsea.mimeutil.MimeUtil;
//import java.io.*;
//import java.net.ConnectException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.nio.charset.Charset;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.security.cert.CertificateException;
//import java.security.cert.X509Certificate;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.*;
//import java.util.Map.Entry;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.net.ssl.*;
//import org.apache.commons.lang3.StringEscapeUtils;
//import org.apache.http.Header;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpException;
//import org.apache.http.HttpHost;
//import org.apache.http.HttpRequest;
//import org.apache.http.HttpRequestInterceptor;
//import org.apache.http.HttpResponse;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.methods.HttpPut;
//import org.apache.http.client.methods.HttpRequestBase;
//import org.apache.http.client.methods.HttpUriRequest;
//import org.apache.http.client.params.ClientPNames;
//import org.apache.http.conn.ClientConnectionManager;
//import org.apache.http.conn.scheme.Scheme;
//import org.apache.http.conn.scheme.SchemeRegistry;
//import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.conn.ssl.X509HostnameVerifier;
//import org.apache.http.cookie.*;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.entity.mime.HttpMultipartMode;
//import org.apache.http.entity.mime.MultipartEntity;
//import org.apache.http.entity.mime.content.FileBody;
//import org.apache.http.entity.mime.content.StringBody;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.impl.cookie.BasicClientCookie;
//import org.apache.http.impl.cookie.BrowserCompatSpec;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.params.CoreProtocolPNames;
//import org.apache.http.params.HttpConnectionParams;
//import org.apache.http.params.HttpParams;
//import org.apache.http.protocol.BasicHttpContext;
//import org.apache.http.protocol.ExecutionContext;
//import org.apache.http.protocol.HttpContext;
//import org.apache.http.util.EntityUtils;
//import org.joda.time.DateTime;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import seo.util.exception.TaskStoppedException;
//import seo.util.exception.SubmissionTimeoutException;
//import seo.util.TextUtility;
//import seo.utilities.concurrent.ManualResetEvent;
//import seo.util.diff_match_patch;
//import seo.utilities.http.NukeRedirectStrategy;
//
///**
// *
// * @author GOD
// */
//public class NukeHttpClient extends DefaultHttpClient {
//
//    private String rawBody;
//    private org.jsoup.nodes.Document body;
//    private Charset charset;
//    private HttpRequestBase currentReq;
//    private HashMap<String, String> reqParams;
//    private HttpContext localContext;
//    private HashMap<String, String> httpParams;
//    private HttpContext context;
//    private String currentUrl = "";
//    private Boolean isStopped = false;
//    private int timeout = 0;
//    private ManualResetEvent responseCompletedEvent;
//    private HashMap<String, String> headers;
//    private String defaultUserAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
//    private ExecutorService service;
//    private final diff_match_patch matcher;
//
//    public NukeHttpClient() {
//        super();
//        matcher = new diff_match_patch();
//        matcher.Match_Threshold = (float)0.4;
//
//        reqParams = new HashMap<String, String>();
//        //httpParams = new LinkedList<NameValuePair>();
//        httpParams = new HashMap<String, String>();
//        context = new BasicHttpContext();
//        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
//        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
//    }
//
//    public NukeHttpClient(boolean SSL) throws NoSuchAlgorithmException, KeyManagementException {
//        super();
//        matcher = new diff_match_patch();
//        matcher.Match_Threshold = (float)0.4;
//
//        //this.service = Executors.newSingleThreadExecutor();
//        SSLContext ctx = SSLContext.getInstance("TLS");
//
//        X509TrustManager tm = new X509TrustManager() {
//
//            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//            }
//
//            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//            }
//
//            public X509Certificate[] getAcceptedIssuers() {
//                return null;
//            }
//        };
//        X509HostnameVerifier verifier = new X509HostnameVerifier() {
//
//            @Override
//            public void verify(String string, SSLSocket ssls) throws IOException {
//            }
//
//            @Override
//            public void verify(String string, X509Certificate xc) throws SSLException {
//            }
//
//            @Override
//            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
//            }
//
//            @Override
//            public boolean verify(String string, SSLSession ssls) {
//                return true;
//            }
//        };
//        reqParams = new HashMap<String, String>();
//        //httpParams = new LinkedList<NameValuePair>();
//        httpParams = new HashMap<String, String>();
//        context = new BasicHttpContext();
//        ctx.init(null, new TrustManager[]{tm}, null);
//        SSLSocketFactory ssf = new SSLSocketFactory(ctx);
//        ssf.setHostnameVerifier(verifier);
//        ClientConnectionManager ccm = this.getConnectionManager();
//        SchemeRegistry sr = ccm.getSchemeRegistry();
//        sr.register(new Scheme("https", ssf, 443));
//        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
//        //this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
//
//
//
//        CookieSpecFactory csf = new CookieSpecFactory() {
//
//            public CookieSpec newInstance(HttpParams params) {
//                return new BrowserCompatSpec() {
//
//                    @Override
//                    public void validate(Cookie cookie, CookieOrigin origin)
//                            throws MalformedCookieException {
//                        // Oh, I am easy
//                        // allow all cookies
//                        //log.debug("custom validate");
//                    }
//                };
//            }
//        };
//        this.getCookieSpecs().register("easy", csf);
//        this.getParams().setParameter(
//                ClientPNames.COOKIE_POLICY, "easy");
//        init();
//    }
//
//    public void setRequestExecutorService(ExecutorService service) {
//        this.service = service;
//    }
//
//    public ExecutorService getRequestExecutorService() {
//        return this.service;
//    }
//
//    public void setDefaulUserAgent(String defauUserAgent) {
//        this.defaultUserAgent = defauUserAgent;
//    }
//
//    public void setTimeOut(int timeout) {
//        this.timeout = timeout;
//        HttpParams params = this.getParams();
//        HttpConnectionParams.setConnectionTimeout(params, timeout * 1000);
//        HttpConnectionParams.setSoTimeout(params, timeout * 1000);
//
//    }
//
//    public void init() {
//        this.initInterceptor();
//        this.initRedirectStrategy();
//    }
//
//    public void initRedirectStrategy() {
//        this.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
//        this.setRedirectStrategy(new NukeRedirectStrategy());
//    }
//
//    public void setStopEvent(ManualResetEvent event) {
//        this.responseCompletedEvent = event;
//    }
//
//    public ManualResetEvent getStopEvent() {
//        return this.responseCompletedEvent;
//    }
//
//    public void initInterceptor() {
//        this.headers = new HashMap<String, String>();
//        this.addRequestInterceptor(new HttpRequestInterceptor() {
//
//            public void process(
//                    final HttpRequest request,
//                    final HttpContext context) throws HttpException, IOException {
//                Iterator it = NukeHttpClient.this.headers.entrySet().iterator();
////                request.removeHeaders("Connection");
////                request.removeHeaders("Proxy-Connection");
//                while (it.hasNext()) {
//                    Map.Entry pairs = (Map.Entry) it.next();
//                    request.addHeader((String) pairs.getKey(), (String) pairs.getValue());
//                }
//            }
//        });
//    }
//
//    public int AddRequestHeader(String key, String value) {
//        if (key.equals("User-Agent")) {
//            this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, value);
//        } else {
//            //reqParams.put(key,value);
//            this.headers.put(key, value);
//        }
//        return 1;
//    }
//
//    public int RemoveRequestHeader(String key) {
//        if (key.equals("User-Agent")) {
//            this.getParams().removeParameter(CoreProtocolPNames.USER_AGENT);
//        } else {
//            this.headers.remove(key);
//        }
//        return 1;
//    }
//
//    public void ClearCookies() {
//        this.getCookieStore().clear();
//    }
//
//    public void ClearClientHeaders() {
//        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, defaultUserAgent);
//        this.headers.clear();
//        this.getCookieStore().clear();
//    }
//
//    public void AddHiddenParamsContains(String innerText) {
//        Element myForm = this.FindElement("form", innerText);
//        if (myForm != null) {
//            Elements hiddenInputs = myForm.select("input[type=hidden]");
//            for (Iterator<Element> i = hiddenInputs.iterator(); i.hasNext();) {
//                Element e = i.next();
//                if (e.attr("value") != null) {
//                    AddPostParam(e.attr("name"), e.attr("value"));
//                    // httpParams.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
//                } else {
//                    AddPostParam(e.attr("name"), "");
//                    //httpParams.add(new BasicNameValuePair(e.attr("name"), ""));
//                }
//            }
//        }
//    }
//
//    public void AddFixedPostParam(String outerHtml) {
//        Elements elements;
//
//        Element myForm = this.FindElement("form", outerHtml);
//        if ((myForm != null)) {
//
//            //Input
//            elements = myForm.select("input");
//            if (elements == null) {
//                return;
//            }
//            for (int i = 0; i < elements.size(); i++) {
//                Element e = elements.get(i);
//                if (e.hasAttr("name")) {
//                    if (e.hasAttr("value")) {
//                        AddPostParam(e.attr("name"), e.attr("value"));
//                        //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), elements.get(i).attr("value")));
//                    } else {
//                        AddPostParam(e.attr("name"), "");
//                        //httpParams.add(new BasicNameValuePair(elements.get(i).attr("name"), ""));
//                    }
//                }
//            }
//
//            //select tags
//            elements = myForm.select("select");
//
//            if (elements == null) {
//                return;
//            }
//
//            Elements options = null;
//
//            Iterator<Element> itSelect = elements.iterator();
//            while (itSelect.hasNext()) {
//                Element selectNode = itSelect.next();
//                if (!selectNode.hasAttr("name")) {
//                    break;
//                }
//                options = selectNode.select("option[value]");
//                if (options == null) {
//                    break;
//                }
//
//                Iterator<Element> itOption = options.iterator();
//                while (itOption.hasNext()) {
//                    Element optionNode = itOption.next();
//                    if (optionNode.hasAttr("selected") && optionNode.hasAttr("value")) {
//                        AddPostParam(selectNode.attr("name"), optionNode.attr("value"));
//                        break;
//                        //httpParams.add(new BasicNameValuePair(selectNode.attr("name"), optionNode.attr("value")));
//                    }
//                }
//                if (options.size() > 1) {
//                    int indexRandom = TextUtility.GenerateNumBetween(1, options.size() - 1);
//                    AddPostParam(selectNode.attr("name"), options.get(indexRandom).attr("value"));
//                } else if (options.size() > 0) {
//                    AddPostParam(selectNode.attr("name"), options.last().attr("value"));
//                }
//            }
//        }
//    }
//
//    public void AddHiddenParams(String form) {
//        //Elements myForms = body.select("form#" + formname);
//        Elements myForms = body.select(form);
//        if (myForms.isEmpty() == false) {
//            Element myForm = myForms.first();
//            Elements hiddenInputs = myForm.select("input[type=hidden]");
//            for (Iterator<Element> i = hiddenInputs.iterator(); i.hasNext();) {
//                Element e = i.next();
//                if (e.attr("value") != null) {
//                    httpParams.put(e.attr("name"), e.attr("value"));
//                } else {
//                    httpParams.put(e.attr("name"), "");
//                }
//            }
//        }
//    }
//
//    public void AddPostParam(String name, String value) {
//        httpParams.put(name, value);
//    }
//
//
//    public String GetPostParam(String name) {
//        return httpParams.get(name);
//    }
//
//
//    public void RemovePostParam(String name) {
//        httpParams.remove(name);
//    }
//
//
//    public void ClearPostParams() {
//        httpParams.clear();
//        fileParams.clear();
//    }
//
//    public void ClearReqParams() {
//        reqParams.clear();
//    }
//
//    public void ClearAll() {
//        this.ClearPostParams();
//        this.ClearClientHeaders();
//        this.getCookieStore().clear();
//        //this.
//    }
//
//    public void SetCookie(String name, String value) throws URISyntaxException {
//        this.SetCookie(currentUrl, name, value);
//    }
//
//    public String getCurrentURL() {
//        return currentUrl;
//    }
//
//    public void SetCookie(String url, String name, String value) throws URISyntaxException {
//        URI uri = new URI(RequestHelper.MakeAbsoluteURL(currentUrl, url));
//        BasicClientCookie cookie = new BasicClientCookie(name, value);
//        cookie.setDomain(uri.getHost());
//        this.getCookieStore().addCookie(cookie);
//    }
//
//    public String GetCookie(String url, String name) throws URISyntaxException {
//        URI uri = new URI(url);
//        for (Cookie c : this.getCookieStore().getCookies()) {
//            //if (c.getDomain().equalsIgnoreCase(uri.getHost())) {
//            if ((uri.getHost().toLowerCase().contains(c.getDomain().toLowerCase()))) {
//                if (c.getName().equals(name)) {
//                    return c.getValue();
//                }
//            }
//        }
//        return null;
//    }
//
//    public String GetAllCookieString(String url) throws URISyntaxException {
//        URI uri = new URI(url);
//        String hostName = uri.getHost();
//        int port = uri.getPort();
//        if (port < 0) {
//
//            // Target port will be selected by the proxy.
//            // Use conventional ports for known schemes
//            String scheme = uri.getScheme();
//            if (scheme.equalsIgnoreCase("http")) {
//                port = 80;
//            } else if (scheme.equalsIgnoreCase("https")) {
//                port = 443;
//            } else {
//                port = 0;
//            }
//
//        }
//
//        String path = uri.getPath();
//        boolean isSecure = (port == 80) ? false : true;
//
//        CookieOrigin cookieOrigin = new CookieOrigin(
//                hostName,
//                port,
//                path,
//                isSecure);
//
//        String policy = (String)(this.getParams().getParameter(ClientPNames.COOKIE_POLICY));
//
//        // Get an instance of the selected cookie policy
//        CookieSpec cookieSpec = this.getCookieSpecs().getCookieSpec(policy);
//        // Get all cookies available in the HTTP state
//        List<Cookie> cookies = new ArrayList<Cookie>(this.getCookieStore().getCookies());
//        // Find cookies matching the given origin
//        List<Cookie> matchedCookies = new ArrayList<Cookie>();
//
//        DateTime n = new DateTime();
//        //n.minusMillis(0);
//        Date now = n.toDate();
//
//        for (Cookie cookie : cookies) {
//            if (!cookie.isExpired(now)) {
//                if (cookieSpec.match(cookie, cookieOrigin)) {
//
//                    matchedCookies.add(cookie);
//                }
//            } else {
//            }
//        }
//        // Generate Cookie request headers
//        String allCookies = "";
//        if (!matchedCookies.isEmpty()) {
//            List<Header> headers = cookieSpec.formatCookies(matchedCookies);
//            for (Header header : headers) {
//                allCookies = header.getValue();
//            }
//        }
//        return allCookies;
//    }
//
//    public String getRawBody() {
//        return rawBody;
//    }
//
//    public org.jsoup.nodes.Document getBody() {
//        return body;
//    }
//
//    public void setBody(Document doc) {
//        this.body = doc;
//    }
//
//    public void InternalRedirect(String url, int nRedirect) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpGet httpget = new HttpGet(url);
//        try {
//            if (nRedirect >= 11) {
//                return;
//            }
//            currentReq = httpget;
//            final HttpContext localContext = new BasicHttpContext();
//
//            Future f = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(httpget, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//
//            if (waitResult == false) {
//                httpget.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                httpget.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            responseCompletedEvent.reset();
//            body = Jsoup.parse(rawBody);
//
////            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (localCurrentReq.getURI().isAbsolute()) {
////                currentUrl = localCurrentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            Elements meta = body.select("html head meta[http-equiv]");
//            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                if (checkNoScript(meta)) {
//                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                    String metaURL = match == null ? null : match[2];
//                    this.headers.remove("Referer");
//                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                    InternalRedirect(metaURL, nRedirect + 1);
//                }
//            }
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//
//        }
//    }
//
//    protected boolean checkNoScript(Elements meta) {
//        Elements parents = meta.parents();
//        Iterator<Element> it = parents.iterator();
//        boolean result = true;
//        while (it.hasNext()) {
//            Element parent = it.next();
//            if (parent.tagName().equalsIgnoreCase("noscript")) {
//                result = false;
//                break;
//            }
//        }
//        return result;
//    }
//
//    public void Navigate(String url, boolean autoRedirect) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpGet httpget = new HttpGet(url);
//        try {
//            currentReq = httpget;
//
//            final HttpContext localContext = new BasicHttpContext();
//
//            Future f = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(httpget, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                        if (ex instanceof ConnectException) {
//                        }
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            //responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            responseCompletedEvent.reset();
//            body = Jsoup.parse(rawBody);
////            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (currentReq.getURI().isAbsolute()) {
////                currentUrl = currentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            if (autoRedirect) {
//                Elements meta = body.select("html head meta[http-equiv]");
//                if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                    if (checkNoScript(meta)) {
//                        String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                        String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                        String metaURL = match == null ? null : match[2];
//                        //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                        this.headers.remove("Referer");
//                        InternalRedirect(metaURL, 1);
//                    }
//                }
//            }
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//
//        }
//
//
//    }
//
//    public void PUT(String url, boolean autoRedirect) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpPut httpget = new HttpPut(url);
//        try {
//            currentReq = httpget;
//
//            final HttpContext localContext = new BasicHttpContext();
//
//            Future f = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(httpget, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                        if (ex instanceof ConnectException) {
//                        }
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            //responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    f.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            responseCompletedEvent.reset();
//            body = Jsoup.parse(rawBody);
////            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (currentReq.getURI().isAbsolute()) {
////                currentUrl = currentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            if (autoRedirect) {
//                Elements meta = body.select("html head meta[http-equiv]");
//                if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                    if (checkNoScript(meta)) {
//                        String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                        String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                        String metaURL = match == null ? null : match[2];
//                        //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                        this.headers.remove("Referer");
//                        InternalRedirect(metaURL, 1);
//                    }
//                }
//            }
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//
//        }
//
//
//    }
//
//    public void Navigate(String url) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        this.Navigate(url.trim(), true);
//    }
//
//    public String DownloadString(String url) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException {
//        String result = null;
//        final HttpGet httpget = new HttpGet(url);
//        try {
//            Future task = service.submit(new Callable<String>() {
//
//                @Override
//                public String call() throws Exception {
//                    String result = null;
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(httpget, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            result = EntityUtils.toString(entity, charset);
//                        } else {
//                            result = EntityUtils.toString(entity);
//                        }
//
//                    } catch (IOException ex) {
//                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                        return result;
//                    }
//                }
//            });
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            result = (String) task.get();
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//            return result;
//        }
//
//    }
//
//    public InputStream DownloadGoogleCaptcha(String key) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException {
//        String keyInfoStr = this.DownloadString("http://api.recaptcha.net/noscript?k=" + key);
//        return null;
//    }
//
//    public byte[] DownloadImageInByte(String url) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpGet httpget = new HttpGet(url);
//        byte[] result = null;
//        try {
//
//            Future task = service.submit(new Callable<byte[]>() {
//
//                @Override
//                public byte[] call() throws Exception {
//                    byte[] result = null;
//                    try {
//
//                        HttpResponse response = NukeHttpClient.this.execute(httpget);
//                        HttpEntity entity = response.getEntity();
//                        //byte[] bytes = EntityUtils.toByteArray(entity);
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//
//                        result = EntityUtils.toByteArray(entity);
//
//
//                    } catch (IOException ex) {
//                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                        return result;
//                    }
//                }
//            });
//
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            byte[] bytes = (byte[]) task.get();
//            result = bytes;
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//            return result;
//        }
//    }
//
//    public InputStream DownloadImage(String url) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpGet httpget = new HttpGet(url);
//        InputStream result = null;
//        try {
//
//            Future task = service.submit(new Callable<byte[]>() {
//
//                @Override
//                public byte[] call() throws Exception {
//                    byte[] result = null;
//                    try {
//
//                        HttpResponse response = NukeHttpClient.this.execute(httpget);
//                        HttpEntity entity = response.getEntity();
//                        //byte[] bytes = EntityUtils.toByteArray(entity);
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//
//                        result = EntityUtils.toByteArray(entity);
//
//
//                    } catch (IOException ex) {
//                        //Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        httpget.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                        return result;
//                    }
//                }
//            });
//
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            byte[] bytes = (byte[]) task.get();
//            result = new ByteArrayInputStream(bytes);
//        } finally {
//            httpget.releaseConnection();
//            responseCompletedEvent.reset();
//            return result;
//        }
//    }
//
//    private List<NameValuePair> fileParams = new LinkedList<NameValuePair>();
//
//    public void AddFilePostParam(String param, String fileName) {
//        this.fileParams.add(new BasicNameValuePair(param, fileName));
//    }
//
//    public void MultiPost(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpPost postReq = new HttpPost(url);
//        try {
//            currentReq = postReq;
//            MultipartEntity data = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//
//            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
//            while (it.hasNext()) {
//                Entry<String, String> param = it.next();
//                String name = param.getKey();
//                String value = param.getValue();
//                String[] values = value.split("\\|x\\|");
//                for (int i = 0; i < values.length; i++) {
//                    data.addPart(name, new StringBody(values[i]));
//                }
//            }
//
//
//            //MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
//            for (NameValuePair filePart : fileParams) {
//                File f = new File(filePart.getValue());
//                Collection s = MimeUtil.getMimeTypes(f);
//                if (s != null) {
//                    data.addPart(filePart.getName(), new FileBody(f, s.toString()));
//                } else {
//                    data.addPart(filePart.getName(), new FileBody(f));
//                }
//            }
//
//            //UrlEncodedFormEntity data = new UrlEncodedFormEntity(httpParams);
//            this.ClearPostParams();
//            postReq.setEntity(data);
//
//            final HttpContext localContext = new BasicHttpContext();
//            Future g = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(postReq, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        postReq.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                throw new TaskStoppedException("Task stopped");
//            }
//
//            body = Jsoup.parse(rawBody);
////            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (localCurrentReq.getURI().isAbsolute()) {
////                currentUrl = localCurrentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            Elements meta = body.select("html head meta[http-equiv]");
//            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                if (checkNoScript(meta)) {
//                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                    String metaURL = match == null ? null : match[2];
//                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                    this.headers.remove("Referer");
//                    InternalRedirect(metaURL, 1);
//                }
//            }
//        } finally {
//            postReq.releaseConnection();
//            responseCompletedEvent.reset();
//        }
//    }
//
//    public void MultiPostSlideshare(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
//        final HttpPost postReq = new HttpPost(url);
//        try {
//            currentReq = postReq;
//            MultipartEntity data = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//            data.addPart("Filename", new StringBody(httpParams.get("Filename")));
//            data.addPart("key", new StringBody(httpParams.get("key")));
//            data.addPart("signature", new StringBody(httpParams.get("signature")));
//            data.addPart("AWSAccessKeyId", new StringBody(httpParams.get("AWSAccessKeyId")));
//            data.addPart("acl", new StringBody(httpParams.get("acl")));
//            data.addPart("success_action_status", new StringBody(httpParams.get("success_action_status")));
//            data.addPart("file_id", new StringBody(httpParams.get("file_id")));
//            data.addPart("policy", new StringBody(httpParams.get("policy")));
//
//            //MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
//            for (NameValuePair filePart : fileParams) {
//                File f = new File(filePart.getValue());
//                Collection s = MimeUtil.getMimeTypes(f);
//                if (s != null) {
//                    data.addPart(filePart.getName(), new FileBody(f, s.toString()));
//                } else {
//                    data.addPart(filePart.getName(), new FileBody(f));
//                }
//            }
//
//            data.addPart("Upload", new StringBody(httpParams.get("Upload")));
//
//
//            //UrlEncodedFormEntity data = new UrlEncodedFormEntity(httpParams);
//            this.ClearPostParams();
//            postReq.setEntity(data);
//
//            final HttpContext localContext = new BasicHttpContext();
//            Future g = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(postReq, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        postReq.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                throw new TaskStoppedException("Task stopped");
//            }
//
//            body = Jsoup.parse(rawBody);
////            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (localCurrentReq.getURI().isAbsolute()) {
////                currentUrl = localCurrentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            Elements meta = body.select("html head meta[http-equiv]");
//            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                if (checkNoScript(meta)) {
//                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                    String metaURL = match == null ? null : match[2];
//                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                    this.headers.remove("Referer");
//                    InternalRedirect(metaURL, 1);
//                }
//            }
//        } finally {
//            postReq.releaseConnection();
//            responseCompletedEvent.reset();
//        }
//    }
//
//       public void POSTRaw(String url, String data) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
//        final HttpPost postReq = new HttpPost(url);
//        try {
//            currentReq = postReq;
//            StringEntity entity = new StringEntity(data);
//            this.ClearPostParams();
//            postReq.setEntity(entity);
//            final HttpContext localContext = new BasicHttpContext();
//            Future task = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(postReq, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        postReq.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            responseCompletedEvent.reset();
//            body = Jsoup.parse(rawBody);
//            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            if (localCurrentReq.getURI().isAbsolute()) {
//                currentUrl = localCurrentReq.getURI().toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
//            }
//            Elements meta = body.select("html head meta[http-equiv]");
//            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                if (checkNoScript(meta)) {
//                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                    String metaURL = match == null ? null : match[2];
//                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                    this.headers.remove("Referer");
//                    InternalRedirect(metaURL, 1);
//                }
//            }
//        } finally {
//            postReq.releaseConnection();
//            responseCompletedEvent.reset();
//        }
//    }
//    public int GET(String url, List<NameValuePair> params) {
//        url = RequestHelper.MakeGETRequest(url, params);
//        try {
//            HttpGet httpget = new HttpGet(url);
//            currentReq = httpget;
//            HttpResponse response = this.execute(httpget);
//            HttpEntity entity = response.getEntity();
//
//            ContentType contentType = ContentType.getOrDefault(entity);
//            Charset charset = contentType.getCharset();
//
//            if (charset != null) {
//                rawBody = EntityUtils.toString(entity, charset);
//            } else {
//                rawBody = EntityUtils.toString(entity);
//            }
//
//            body = Jsoup.parse(rawBody);
//
//            return 1;
//        } catch (Exception ex) {
//            //log exepction here
//            return 0;
//        }
//    }
//
//    public void POST(String url) throws UnsupportedEncodingException, IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        url = RequestHelper.MakeAbsoluteURL(currentUrl, url.trim());
//        final HttpPost postReq = new HttpPost(url);
//        try {
//            currentReq = postReq;
//            List<NameValuePair> paramList = new LinkedList<NameValuePair>();
//            Iterator<Entry<String, String>> it = httpParams.entrySet().iterator();
//            while (it.hasNext()) {
//                Entry<String, String> param = it.next();
//                String name = param.getKey();
//                String value = param.getValue();
//                String[] values = value.split("\\|x\\|");
//                for (int i = 0; i < values.length; i++) {
//                    paramList.add(new BasicNameValuePair(name, values[i]));
//                }
//            }
//
//            UrlEncodedFormEntity data = new UrlEncodedFormEntity(paramList);
//            this.ClearPostParams();
//            postReq.setEntity(data);
//            final HttpContext localContext = new BasicHttpContext();
//            Future task = service.submit(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        HttpResponse response;
//                        response = NukeHttpClient.this.execute(postReq, localContext);
//                        HttpEntity entity = response.getEntity();
//
//                        ContentType contentType = ContentType.getOrDefault(entity);
//                        Charset charset = contentType.getCharset();
//
//                        if (charset != null) {
//                            rawBody = EntityUtils.toString(entity, charset);
//                        } else {
//                            rawBody = EntityUtils.toString(entity);
//                        }
//
//                    } catch (Exception ex) {
//                        Logger.getLogger(NukeHttpClient.class.getName()).log(Level.SEVERE, null, ex);
//                    } finally {
//                        postReq.releaseConnection();
//                        responseCompletedEvent.setSource("completed");
//                        responseCompletedEvent.set();
//                    }
//
//                }
//            });
//
//            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.SECONDS);
//            responseCompletedEvent.reset();
//            if (waitResult == false) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new SubmissionTimeoutException("Timeout!");
//            }
//            if (responseCompletedEvent.getSource().equals("cancelled")) {
//                currentReq.abort();
//                try {
//                    task.get();
//                } catch (Exception ex) {
//                }
//                throw new TaskStoppedException("Task stopped");
//            }
//            responseCompletedEvent.reset();
//            body = Jsoup.parse(rawBody);
////            HttpUriRequest localCurrentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
////            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
////            if (localCurrentReq.getURI().isAbsolute()) {
////                currentUrl = localCurrentReq.getURI().toString();
////            } else {
////                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), localCurrentReq.getURI().toString());
////            }
//            HttpRequest currentReq = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
//            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//            URI tmpURI = new URI(currentReq.getRequestLine().getUri());
//            if (tmpURI.isAbsolute()) {
//                currentUrl = tmpURI.toString();
//            } else {
//                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), tmpURI.toString());
//            }
//
//            Elements meta = body.select("html head meta[http-equiv]");
//            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
//                if (checkNoScript(meta)) {
//                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
//                    String[] match = TextUtility.ExtractWhole(content, "[0-9]*\\s*;\\s*(URL|url)=[\"']*([^\"']*)[\"']*");
//                    String metaURL = match == null ? null : match[2];
//                    //metaURL = StringEscapeUtils.escapeHtml4(metaURL);
//                    this.headers.remove("Referer");
//                    InternalRedirect(metaURL, 1);
//                }
//            }
//        } finally {
//            postReq.releaseConnection();
//            responseCompletedEvent.reset();
//        }
//    }
//
//    public void PUT(String url) throws IOException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, URISyntaxException {
//        this.PUT(url, false);
//    }
//
//    public boolean responseContains(String text) {
//        String[] textEls = text.split("\\|");
//        for (int i = 0; i < textEls.length; i++) {
//            if (textEls[i].indexOf("*") == 0) {
//                String[] txt = textEls[i].split("\\*");
//                if (txt.length == 0) {
//                    return false;
//                }
//                if (this.getCurrentURL().contains(txt[1])) {
//                    return true;
//                }
//            } else if (rawBody.contains(textEls[i])) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public Element FindItem(String selector, boolean forward) {
//        Elements els = body.select(selector);
//        if (els.isEmpty()) {
//            return null;
//        } else {
//            if (forward == true) {
//                return els.first();
//            } else {
//                return els.last();
//            }
//        }
//    }
//
//    public Elements FindElements(String selector) {
//        Elements els = body.select(selector);
//        return els;
//    }
//
//    public Element FindElement(String selector) {
//        return FindItem(selector, false);
//    }
//
//    public Element FindElement(String tag, String innerText) {
//        return this.FindElement(tag, innerText, false);
//    }
//
//    public List<Element> FindElements(String tag, String innerText) {
//        List<Element> result = new LinkedList<Element>();
//        Elements es = body.select(tag);
//        String lInnerText = innerText.toLowerCase();
//        int s = es.size();
//
//        for (int i = 0; i < s; i++) {
//            Element e = es.get(i);
//            if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                result.add(e);
//            }
//        }
//        return result;
////        if (result.size() == 0) {
////            return null;
////        } else {
////            return result;
////        }
//
//    }
//
//    public Element FindElement(String tag, String innerText, boolean first) {
//        return this.FindElement(tag, innerText, body, first);
//    }
//
//    public Element FindElement(String selector, Element parent, boolean first) {
//        Elements els = parent.select(selector);
//        if (els.isEmpty()) {
//            return null;
//        } else {
//            if (first == true) {
//                return els.first();
//            } else {
//                return els.last();
//            }
//        }
//
//    }
//
//    public Element FindElement(String tag, String innerText, Element parent, boolean first) {
//        Elements es = parent.select(tag);
//        String lInnerText = innerText.toLowerCase();
//        int s = es.size();
//        if (first == true) {
//            for (int i = 0; i < s; i++) {
//                Element e = es.get(i);
//                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                    return e;
//                }
//            }
//        } else {
//            for (int i = s - 1; i >= 0; i--) {
//                Element e = es.get(i);
//                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
//                    return e;
//                }
//            }
//        }
//        return null;
//    }
//
//    public Element FindElement(Element parents, String selector) {
//        return this.FindElement(parents, selector, true);
//    }
//
//    public Element FindElement(Element parents, String selector, boolean first) {
//        Elements result = parents.select(selector);
//        if (result.isEmpty()) {
//            return null;
//        } else {
//            if (first == true) {
//                return result.first();
//            } else {
//                return result.last();
//            }
//        }
//    }
//
//    public void stop() {
//        synchronized (this.isStopped) {
//            this.isStopped = true;
//            if (currentReq != null) {
//                currentReq.abort();
//            }
//        }
//    }
//
//    public void shutdown() {
//        //this.service.shutdown();
//        //this.service = null;
//        this.getConnectionManager().shutdown();
//    }
//
//    private String FindUrlByTitleFuzzy(String title) throws Exception {
////        String shortTitle = TextUtility.Left(title, title.length() / 2);
////        Element el = FindElement("a", title, true);
////        return el.attr("href");
//        int length = title.length();
//        int left = 0;
//        int right = length - 1;
//        int chosenLength = -1;
//        while (left <= right) {
//            int mid = left + (right - left) / 2;
//            String shortTitle = TextUtility.Left(title, mid + 1);
//            List<Element> es = FindElementsFuzzy("a", shortTitle);
//            if (es.size() > 0) {
//                if (mid > chosenLength) chosenLength = mid + 1;
//            }
//            if (es.size() > 0) {
//                left = mid + 1;
//            } else {
//                right = mid - 1;
//            }
//        }
//        if (chosenLength > 0) {
//            Element el = FindElementFuzzy("a", TextUtility.Left(title, chosenLength));
//            if (el != null) {
//                return el.attr("href");
//            }
//            else {
//                throw new Exception("No Link found!");
//            }
//        } else {
//            throw new Exception("No Link found!");
//        }
//    }
//
//    public String FindUrlByTitle(String title) throws Exception{
//        int length = title.length();
//        int left = 0;
//        int right = length - 1;
//        left = left + (right - left) / 2;
//        int chosenLength = -1;
//        while (left <= right) {
//            int mid = left + (right - left) / 2;
//            String shortTitle = TextUtility.Left(title, mid + 1);
//            List<Element> es = FindElements("a", shortTitle);
//            if (es.size() > 0) {
//                if (mid > chosenLength) chosenLength = mid + 1;
//            }
//            if (es.size() > 0) {
//                left = mid + 1;
//            } else {
//                right = mid - 1;
//            }
//        }
//        if (chosenLength > 0) {
//            Element el = FindElement("a", TextUtility.Left(title, chosenLength));
//            if (el != null) {
//                return el.attr("href");
//            } else {
//                return FindUrlByTitleFuzzy(title);
//                //throw new Exception("No Link found!");
//            }
//        } else return FindUrlByTitleFuzzy(title);
//    }
//
//    public Element FindElementFuzzy(String tag, String innerText) {
//        return this.FindElementFuzzy(tag, innerText, false);
//    }
//
//    public List<Element> FindElementsFuzzy(String tag, String innerText) {
//        List<Element> result = new LinkedList<Element>();
//        Elements es = body.select(tag);
//        String lInnerText = innerText.toLowerCase();
//        lInnerText = TextUtility.Left(lInnerText, 32);
//        int s = es.size();
//
//        for (int i = 0; i < s; i++) {
//            Element e = es.get(i);
////            if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
////                result.add(e);
////            }
//            String html = e.outerHtml().toLowerCase();
//            int location = matcher.match_main(html, lInnerText, 0);
//            if (location != -1) {
//                result.add(e);
//            }
//        }
//        return result;
////        if (result.size() == 0) {
////            return null;
////        } else {
////            return result;
////        }
//
//    }
//
//    public Element FindElementFuzzy(String tag, String innerText, boolean first) {
//        return this.FindElementFuzzy(tag, innerText, body, first);
//    }
//
//    private Element FindElementFuzzy(String tag, String innerText, Element parent, boolean first) {
//        Elements es = parent.select(tag);
//        String lInnerText = innerText.toLowerCase();
//        lInnerText = TextUtility.Left(lInnerText, 32);
//        int s = es.size();
//        if (first == true) {
//            for (int i = 0; i < s; i++) {
//                Element e = es.get(i);
//                String html = e.outerHtml().toLowerCase();
////                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
////                    return e;
////                }
//                int location = matcher.match_main(html, lInnerText, 0);
//                if (location != -1) return e;
//            }
//        } else {
//            for (int i = s - 1; i >= 0; i--) {
//                Element e = es.get(i);
//                String html = e.outerHtml().toLowerCase();
////                if (e.outerHtml().toLowerCase().contains(lInnerText.toLowerCase())) {
////                    return e;
////                }
//                int location = matcher.match_main(html, lInnerText, 0);
//                if (location != -1) return e;
//            }
//        }
//        return null;
//    }
//}
