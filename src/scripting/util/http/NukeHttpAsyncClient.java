/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.conn.ClientAsyncConnectionManager;
import org.apache.http.nio.conn.scheme.AsyncScheme;
import org.apache.http.nio.conn.scheme.AsyncSchemeRegistry;
import org.apache.http.nio.conn.ssl.SSLLayeringStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.util.TextUtility;
import scripting.util.concurrent.ManualResetEvent;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author GOD
 */
public class NukeHttpAsyncClient extends DefaultHttpAsyncClient {

    class ResponseFutureCallback implements FutureCallback<HttpResponse> {

        private ManualResetEvent event;

        public ResponseFutureCallback(ManualResetEvent event) {
            this.event = event;
        }

        @Override
        public void completed(HttpResponse t) {
            this.event.setSource("completed");
            this.event.set();
        }

        @Override
        public void failed(Exception excptn) {
            this.event.setSource("failed");
            this.event.set();
        }

        @Override
        public void cancelled() {
            //this.sema.setSource("cancelled");
            //this.sema.release();
            int x = 0;
        }
    }
    private String rawBody;
    private org.jsoup.nodes.Document body;
    private Charset charset;
    private HttpRequestBase currentReq;
    private HashMap<String, String> reqParams;
    private HttpContext localContext;
    private List<NameValuePair> httpParams;
    private HttpContext context;
    private String currentUrl = "";
    private Boolean isStopped = false;
    private ManualResetEvent responseCompletedEvent;
    private HashMap<String, String> headers;
    private String defaultUserAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    private int timeout = 0;

    public NukeHttpAsyncClient() throws IOReactorException {
        super();
        this.responseCompletedEvent = new ManualResetEvent(false);
        reqParams = new HashMap<String, String>();
        httpParams = new LinkedList<NameValuePair>();
        context = new BasicHttpContext();
        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, defaultUserAgent);
        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        init();
        //initInterceptor();        
    }

    public void setTimeOut(int timeout) {
        this.timeout = timeout;
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

    public NukeHttpAsyncClient(boolean SSL) throws NoSuchAlgorithmException, KeyManagementException, IOReactorException {
        super();
        this.responseCompletedEvent = new ManualResetEvent(false);
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
        httpParams = new LinkedList<NameValuePair>();
        context = new BasicHttpContext();
        ctx.init(null, new TrustManager[]{tm}, null);
        SSLLayeringStrategy ssf = new SSLLayeringStrategy(ctx, verifier);
        ClientAsyncConnectionManager ccm = this.getConnectionManager();
        AsyncSchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new AsyncScheme("https", 443, ssf));
        this.getParams().setParameter(CoreProtocolPNames.USER_AGENT, defaultUserAgent);
        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        //initInterceptor();
        init();
    }

    public void initInterceptor() {
        this.headers = new HashMap<String, String>();
        this.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                Iterator it = NukeHttpAsyncClient.this.headers.entrySet().iterator();
//                request.removeHeaders("Connection");
//                request.removeHeaders("Proxy-Connection");
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    request.addHeader((String) pairs.getKey(), (String) pairs.getValue());
                }
            }
        });
    }

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
                    httpParams.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
                } else {
                    httpParams.add(new BasicNameValuePair(e.attr("name"), ""));
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
                    httpParams.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
                } else {
                    httpParams.add(new BasicNameValuePair(e.attr("name"), ""));
                }
            }
        }
    }

    public void AddPostParam(String name, String value) {
        String[] values = value.split("\\|x\\|");
        for (int i = 0; i < values.length; i++) {
            httpParams.add(new BasicNameValuePair(name, values[i]));
        }
    }

    public void RemovePostParam(String name) {
        for (Iterator<NameValuePair> i = httpParams.iterator(); i.hasNext();) {
            NameValuePair c = i.next();
            if (c.getName().equals(name)) {
                httpParams.remove(c);
                break;
            }
        }
    }

    public void RemovePostParams(String name) {
        for (Iterator<NameValuePair> i = httpParams.iterator(); i.hasNext();) {
            NameValuePair c = i.next();
            if (c.getName().equals(name)) {
                httpParams.remove(c);
            }
        }
    }

    public void ClearPostParams() {
        httpParams.clear();
    }

    public void ClearReqParams() {
        reqParams.clear();
    }

    public void ClearAll() {
        this.ClearPostParams();
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

    public String getRawBody() {
        return rawBody;
    }

    public org.jsoup.nodes.Document getBody() {
        return body;
    }

    public void InternalRedirect(String url, int nRedirect) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException, IOException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);

        try {
            if (nRedirect >= 11) {
                return;
            }
            HttpGet httpget = new HttpGet(url);
            //httpget.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
            //httpget.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            currentReq = httpget;
            //responseCompletedEvent.acquire();
            //BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(new HttpHost(httpget.getURI().getHost()), httpget);        
            //        Future<HttpResponse> future = this.execute(
            //                    producer,
            //                    new BasicAsyncResponseConsumer(), new ResponseFutureCallback(responseSema));  
            HttpContext localContext = new BasicHttpContext();
            Future<HttpResponse> future = this.execute(httpget, localContext, new ResponseFutureCallback(responseCompletedEvent));
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.MILLISECONDS);
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();
            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                rawBody = EntityUtils.toString(entity, charset);
            } else {
                rawBody = EntityUtils.toString(entity);
            }

            body = Jsoup.parse(rawBody);

            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (currentReq.getURI().isAbsolute()) {
                currentUrl = currentReq.getURI().toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
            }
            Elements meta = body.select("html head meta");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                String[] match = TextUtility.ExtractAll(content, "[0-9]*;(URL|url)=[\"']*([^\"']*)[\"']*");
                String metaURL = match == null ? null : match[2];
                this.headers.remove("Referer");
                metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                InternalRedirect(metaURL, nRedirect + 1);
            }
        } finally {
            //httpget.releaseConnection();
            responseCompletedEvent.reset();

        }
    }

    public void Navigate(String url, boolean autoRedirect) throws InterruptedException, InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException, IOException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        HttpGet httpget = new HttpGet(url);
        try {
            //httpget.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
            //httpget.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            currentReq = httpget;
            //responseCompletedEvent.acquire();
            //BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(new HttpHost(httpget.getURI().getHost()), httpget);        
            //        Future<HttpResponse> future = this.execute(
            //                    producer,
            //                    new BasicAsyncResponseConsumer(), new ResponseFutureCallback(responseSema));  
            HttpContext localContext = new BasicHttpContext();
            Future<HttpResponse> future = this.execute(httpget, localContext, new ResponseFutureCallback(responseCompletedEvent));
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.MILLISECONDS);
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();
            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                rawBody = EntityUtils.toString(entity, charset);
            } else {
                rawBody = EntityUtils.toString(entity);
            }


            body = Jsoup.parse(rawBody);
            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (currentReq.getURI().isAbsolute()) {
                currentUrl = currentReq.getURI().toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
            }
            if (autoRedirect) {
                Elements meta = body.select("html head meta");
                if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                    String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                    String[] match = TextUtility.ExtractAll(content, "[0-9]*;(URL|url)=[\"']*([^\"']*)[\"']*");
                    String metaURL = match == null ? null : match[2];
                    metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                    this.headers.remove("Referer");
                    InternalRedirect(metaURL, 1);
                }
            }
        } finally {
            //httpget.releaseConnection();
            responseCompletedEvent.reset();

        }
    }

    public void Navigate(String url) throws IOException, InterruptedException, ExecutionException, TaskStoppedException, SubmissionTimeoutException {
        this.Navigate(url, true);
    }

    public String DownloadString(String url) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, ExecutionException, IOException {
        //url = RequestHelper.MakeAbsoluteURL(currentUrl, url);                        
        String result = "";
        HttpGet httpget = new HttpGet(url);
        try {
            HttpContext localContext = new BasicHttpContext();
            Future<HttpResponse> future = this.execute(httpget, localContext, new ResponseFutureCallback(responseCompletedEvent));
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.MILLISECONDS);
            if (waitResult == false) {
                httpget.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                httpget.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();
            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                result = EntityUtils.toString(entity, charset);
            } else {
                result = EntityUtils.toString(entity);
            }
        } finally {
            //httpget.releaseConnection();
            responseCompletedEvent.reset();
            return result;

        }
    }

    public InputStream DownloadGoogleCaptcha(String key) throws IOException, InterruptedException, SubmissionTimeoutException, SubmissionTimeoutException, TaskStoppedException, ExecutionException {
        String keyInfoStr = this.DownloadString("http://api.recaptcha.net/noscript?k=" + key);
        return null;
    }

    public InputStream DownloadImage(String url) throws InterruptedException, ExecutionException, IOException, TaskStoppedException, SubmissionTimeoutException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);

        HttpGet httpget = new HttpGet(url);
        try {
            currentReq = httpget;
            //responseCompletedEvent.acquire();
//            BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(new HttpHost(httpget.getURI().getHost()), httpget);        
//            Future<HttpResponse> future = this.execute(
//                        producer,
//                        new BasicAsyncResponseConsumer(), new ResponseFutureCallback(responseSema));  
            Future<HttpResponse> future = this.execute(httpget, new ResponseFutureCallback(responseCompletedEvent));
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.MILLISECONDS);
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();
            HttpEntity entity = response.getEntity();

            byte[] bytes = EntityUtils.toByteArray(entity);
            InputStream in = new ByteArrayInputStream(bytes);
            return in;
        } finally {
            //httpget.releaseConnection();
            responseCompletedEvent.reset();
        }

    }

    public void POST(String url) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, SubmissionTimeoutException {
        url = RequestHelper.MakeAbsoluteURL(currentUrl, url);
        HttpPost postReq = new HttpPost(url);
        try {
            currentReq = postReq;
            UrlEncodedFormEntity data = new UrlEncodedFormEntity(httpParams);
            this.ClearPostParams();
            postReq.setEntity(data);

            //responseCompletedEvent.acquire();
//        BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(new HttpHost(postReq.getURI().getHost()), postReq);        
//        Future<HttpResponse> future = this.execute(
//                    producer,
//                    new BasicAsyncResponseConsumer(), new ResponseFutureCallback(responseSema));  
            HttpContext localContext = new BasicHttpContext();
            Future<HttpResponse> future = this.execute(postReq, localContext, new ResponseFutureCallback(responseCompletedEvent));
            boolean waitResult = responseCompletedEvent.waitOne(timeout, TimeUnit.MILLISECONDS);
            if (waitResult == false) {
                currentReq.abort();
                throw new SubmissionTimeoutException("Timeout!");
            }
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();

            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                rawBody = EntityUtils.toString(entity, charset);
            } else {
                rawBody = EntityUtils.toString(entity);
            }

            body = Jsoup.parse(rawBody);
            HttpUriRequest currentReq = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (currentReq.getURI().isAbsolute()) {
                currentUrl = currentReq.getURI().toString();
            } else {
                currentUrl = RequestHelper.MakeAbsoluteURL(currentHost.toURI().toString(), currentReq.getURI().toString());
            }
            Elements meta = body.select("html head meta");
            if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
                String content = StringEscapeUtils.unescapeHtml4(meta.attr("content"));
                String[] match = TextUtility.ExtractAll(content, "[0-9]*;(URL|url)=[\"']*([^\"']*)[\"']*");
                String metaURL = match == null ? null : match[2];
                metaURL = StringEscapeUtils.escapeHtml4(metaURL);
                this.headers.remove("Referer");
                InternalRedirect(metaURL, 1);
            }
        } finally {
            //postReq.releaseConnection();
            responseCompletedEvent.reset();
        }
    }

    public void GET(String url, List<NameValuePair> params) throws InterruptedException, ExecutionException, IOException, TaskStoppedException {
        url = RequestHelper.MakeGETRequest(url, params);
        try {
            HttpGet httpget = new HttpGet(url);
            currentReq = httpget;
            //responseCompletedEvent.acquire();
//            BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(new HttpHost(httpget.getURI().getHost()), httpget);        
//            Future<HttpResponse> future = this.execute(
//                        producer,
//                        new BasicAsyncResponseConsumer(), new ResponseFutureCallback(responseSema));  
            HttpContext localContext = new BasicHttpContext();
            Future<HttpResponse> future = this.execute(httpget, localContext, new ResponseFutureCallback(responseCompletedEvent));
            responseCompletedEvent.waitOne();
            if (responseCompletedEvent.getSource().equals("cancelled")) {
                currentReq.abort();
                throw new TaskStoppedException("Task stopped");
            }
            HttpResponse response = future.get();

            HttpEntity entity = response.getEntity();

            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset();

            if (charset != null) {
                rawBody = EntityUtils.toString(entity, charset);
            } else {
                rawBody = EntityUtils.toString(entity);
            }

            body = Jsoup.parse(rawBody);
        } finally {
            responseCompletedEvent.reset();
        }
    }

    public int PUT(String url) {
        return 1;
    }

    public boolean responseContains(String text) {
        String[] textEls = text.split("\\|");
        for (int i = 0; i < textEls.length; i++) {
            if (textEls[i].indexOf("*") == 0) {
                String[] txt = textEls[i].split("\\*");
                if (txt.length == 0) {
                    return false;
                }
                if (this.getCurrentURL().contains(txt[1])) {
                    return true;
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

    public Element FindElement(String selector) {
        return FindItem(selector, false);
    }

    public Element FindElement(String tag, String innerText) {
        return this.FindElement(tag, innerText, false);
    }

    public Element FindElement(String tag, String innerText, boolean first) {
        return this.FindElement(tag, innerText, body, first);
    }

    public Element FindElement(String tag, String innerText, Element parent, boolean first) {
        Elements es = parent.select(tag);
        String lInnerText = innerText.toLowerCase();
        int s = es.size();
        if (first == true) {
            for (int i = 0; i < s; i++) {
                Element e = es.get(i);
                if (e.outerHtml().toLowerCase().contains(lInnerText)) {
                    return e;
                }
            }
        } else {
            for (int i = s - 1; i >= 0; i--) {
                Element e = es.get(i);
                if (e.outerHtml().toLowerCase().contains(lInnerText)) {
                    return e;
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

    public String HandleCaptcha(String url) {
        return null;
    }

    public void stop() {
        this.responseCompletedEvent.setSource("cancelled");
        this.responseCompletedEvent.set();
//        
//        synchronized (this.isStopped) {
//            this.isStopped = true;
//            this.responseSema.setSource("cancelled");
//            this.responseSema.release();
//            /*if (currentReq != null) {
//                currentReq.abort();
//            }*/
//        }
    }
}
