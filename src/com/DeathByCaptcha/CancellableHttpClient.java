package com.DeathByCaptcha;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.universalchardet.UniversalDetector;
import scripting.util.concurrent.ManualResetEvent;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Death by Captcha HTTP API client.
 *
 * @author Sergey Kolchin <ksa242@gmail.com>
 */
public class CancellableHttpClient extends Client {

    final static public String CRLF = "\r\n";
    //final static public String SERVER_URL = "http://api.dbc.me/api";
    final static public String SERVER_URL = "http://api.deathbycaptcha.com/api";
//    final static public String SERVER_URL = "http://142.4.209.47:12345/api";
    /**
     * Proxy to use, defaults to none.
     */
    public Proxy proxy = Proxy.NO_PROXY;
    private ExecutorService service;
    private ManualResetEvent stopEvent;

    public void setExecutorService(ExecutorService service) {
        this.service = service;
    }

    public void setStopEvent(ManualResetEvent event) {
        this.stopEvent = event;
    }

    public ManualResetEvent getStopEvent() {
        return this.stopEvent;
    }

    private class HttpClientCaller {

        final static public String RESPONSE_CONTENT_TYPE = "application/json";
        public String response = null;
        public int statusCode;
        public boolean success = true;

        public String call(Proxy proxy, URL url, byte[] payload, String contentType, Date deadline)
                throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, Exception, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
            //String response = null;
            final DefaultHttpClient client = new DefaultHttpClient();
            try {


                //HttpHost myproxy = new HttpHost("127.0.0.1", 8888);
                //client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, myproxy);
                if (!((proxy.type() == Proxy.Type.DIRECT) && (proxy.address() == null))) {
                    HttpHost myproxy = new HttpHost(((InetSocketAddress) (proxy.address())).getHostName(), ((InetSocketAddress) (proxy.address())).getPort());
                    client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, myproxy);
                }
                client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, API_VERSION);
                //client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"); 
                while (deadline.after(new Date()) && null != url && null == response) {
                    final HttpRequestBase req;

                    if (0 < payload.length) {
                        req = new HttpPost(url.toURI());
                        req.addHeader("Content-Type", contentType);
                        req.addHeader("Accept", HttpClientCaller.RESPONSE_CONTENT_TYPE);
                        ((HttpPost) req).setEntity(new ByteArrayEntity(payload));

                        payload = new byte[0];
                    } else {
                        req = new HttpGet(url.toURI());
                        req.addHeader("Content-Type", contentType);
                        req.addHeader("Accept", HttpClientCaller.RESPONSE_CONTENT_TYPE);
                    }
                    currentReq = req;
                    success = true;
                    statusCode = 0;
                    url = null;
                    Future f = service.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                UniversalDetector detector = new UniversalDetector(null);
                                HttpResponse res;

                                res = client.execute(req);


                                statusCode = res.getStatusLine().getStatusCode();
                                switch (statusCode) {
                                    case HttpURLConnection.HTTP_FORBIDDEN:
                                        throw new AccessDeniedException("Access denied, check your credentials and/or balance");

                                    case HttpURLConnection.HTTP_BAD_REQUEST:
                                        throw new InvalidCaptchaException("CAPTCHA was rejected, check if it's a valid image");

                                    case HttpURLConnection.HTTP_UNAVAILABLE:
                                        throw new ServiceOverloadException("CAPTCHA was rejected due to service overload, try again later");


                                }
                                HttpEntity entity = res.getEntity();
                                byte[] responseData = EntityUtils.toByteArray(entity);
                                detector.handleData(responseData, 0, responseData.length);
                                detector.dataEnd();
                                String encoding = detector.getDetectedCharset();
                                System.out.println(encoding);
                                if (encoding == null) {
                                    encoding = "UTF-8";
                                }
                                //response = EntityUtils.toString(entity);
                                response = new String(responseData, encoding);
                                System.out.println(response);
                            } catch (java.lang.Exception e) {
                                success = false;
                                //System.out.println(e);
                            } finally {
                                stopEvent.setSource("completed");
                                stopEvent.set();
                            }
                        }
                    });
                    boolean waitResult = stopEvent.waitOne(0, TimeUnit.SECONDS);
                    if (waitResult == false) {
                        currentReq.abort();
                        try {
                            f.get();
                        } catch (ExecutionException ex) {
                            Logger.getLogger(CancellableHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        throw new SubmissionTimeoutException("Timeout!");
                    }
                    //stopEvent.reset();
                    if (stopEvent.getSource().equals("cancelled")) {
                        req.abort();
                        try {
                            f.get();
                        } catch (ExecutionException ex) {
                            Logger.getLogger(CancellableHttpClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        throw new TaskStoppedException("Task stopped");
                    }

                }

                if (success) {
                    return response;
                } else {
                    switch (statusCode) {
                        case HttpURLConnection.HTTP_FORBIDDEN:
                            throw new AccessDeniedException("Access denied, check your credentials and/or balance");

                        case HttpURLConnection.HTTP_BAD_REQUEST:
                            throw new InvalidCaptchaException("CAPTCHA was rejected, check if it's a valid image");

                        case HttpURLConnection.HTTP_UNAVAILABLE:
                            throw new ServiceOverloadException("CAPTCHA was rejected due to service overload, try again later");
                        default:
                            throw new IOException("Captcha Error");
                    }
                }
            } finally {
                stopEvent.reset();
                client.getConnectionManager().shutdown();
            }
            //return response;
        }
    }

    protected JSONObject call(String cmd, byte[] data, String contentType)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        this.log("SEND", cmd);
        URL url = null;
        try {
            url = new URL(CancellableHttpClient.SERVER_URL + '/' + cmd);
        } catch (java.lang.Exception e) {
            throw new IOException("Invalid API command " + cmd);
        }
        String response = (new HttpClientCaller()).call(
                this.proxy,
                url,
                data,
                (null != contentType ? contentType : "application/x-www-form-urlencoded"),
                new Date(System.currentTimeMillis() + Client.DEFAULT_TIMEOUT * 1000));
        this.log("RECV", response);
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new IOException("Invalid API response");
        }
    }

    protected JSONObject call(String cmd, byte[] data)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        return this.call(cmd, data, null);
    }

    protected JSONObject call(String cmd, JSONObject args)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        StringBuilder data = new StringBuilder();
        java.util.Iterator args_keys = args.keys();
        String k = null;
        while (args_keys.hasNext()) {
            k = args_keys.next().toString();
            try {
                data.append(URLEncoder.encode(k, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new JSONObject();
            }
            data.append("=");
            try {
                data.append(URLEncoder.encode(args.optString(k, ""), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new JSONObject();
            }
            if (args_keys.hasNext()) {
                data.append("&");
            }
        }
        return this.call(cmd, data.toString().getBytes());
    }

    protected JSONObject call(String cmd)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        return this.call(cmd, new byte[0]);
    }

    /**
     * @see com.DeathByCaptcha.Client#Client(String, String)
     */
    public CancellableHttpClient(String username, String password) {
        super(username, password);
    }

    /**
     * @see com.DeathByCaptcha.Client#close
     */
    public void close() {
        if (currentReq != null) {
            currentReq.abort();
        }
    }

    /**
     * @see com.DeathByCaptcha.Client#connect
     */
    public boolean connect()
            throws IOException {
        return true;
    }

    /**
     * @see com.DeathByCaptcha.Client#getUser
     */
    @Override
    public User getUser()
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        return new User(this.call("user", this.getCredentials()));
    }

    /**
     * @see com.DeathByCaptcha.Client#upload
     */
    @Override
    public Captcha upload(byte[] img)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        String boundary = null;
        try {
            boundary = (new java.math.BigInteger(1, (java.security.MessageDigest.getInstance("SHA1")).digest(
                    (new java.util.Date()).toString().getBytes()))).toString(16);
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }

        byte[] hdr = ("--" + boundary + CancellableHttpClient.CRLF + "Content-Disposition: form-data; name=\"username\"" + CancellableHttpClient.CRLF + "Content-Type: text/plain" + CancellableHttpClient.CRLF + "Content-Length: " + this._username.length() + CancellableHttpClient.CRLF + CancellableHttpClient.CRLF + this._username + CancellableHttpClient.CRLF
                + "--" + boundary + CancellableHttpClient.CRLF + "Content-Disposition: form-data; name=\"password\"" + CancellableHttpClient.CRLF + "Content-Type: text/plain" + CancellableHttpClient.CRLF + "Content-Length: " + this._password.length() + CancellableHttpClient.CRLF + CancellableHttpClient.CRLF + this._password + CancellableHttpClient.CRLF
                + "--" + boundary + CancellableHttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + CancellableHttpClient.CRLF + "Content-Type: text/plain" + CancellableHttpClient.CRLF + CancellableHttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + CancellableHttpClient.CRLF
                + "--" + boundary + CancellableHttpClient.CRLF + "Content-Disposition: form-data; name=\"captchafile\"; filename=\"captcha\"" + CancellableHttpClient.CRLF + "Content-Type: application/octet-stream" + CancellableHttpClient.CRLF + "Content-Length: " + img.length + CancellableHttpClient.CRLF + CancellableHttpClient.CRLF).getBytes();
        byte[] ftr = (CancellableHttpClient.CRLF + "--" + boundary + "--").getBytes();
        byte[] body = new byte[hdr.length + img.length + ftr.length];
        System.arraycopy(hdr, 0, body, 0, hdr.length);
        System.arraycopy(img, 0, body, hdr.length, img.length);
        System.arraycopy(ftr, 0, body, hdr.length + img.length, ftr.length);

        Captcha c = new Captcha(this.call("captcha", body,
                "multipart/form-data; boundary=" + boundary));
        return c.isUploaded() ? c : null;
    }

    /**
     * @see com.DeathByCaptcha.Client#getCaptcha
     */
    @Override
    public Captcha getCaptcha(int id)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        return new Captcha(this.call("captcha/" + id));
    }

    /**
     * @see com.DeathByCaptcha.Client#report
     */
    @Override
    public boolean report(int id)
            throws IOException, com.DeathByCaptcha.Exception, URISyntaxException, InterruptedException, TaskStoppedException, SubmissionTimeoutException {
        return !(new Captcha(this.call("captcha/" + id + "/report",
                this.getCredentials()))).isCorrect();
    }
}
