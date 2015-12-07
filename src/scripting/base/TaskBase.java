/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.base;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.nodes.Element;
import scripting.captcha.ReCaptchaInfo;
import scripting.data.entity.ProjectType;
import scripting.data.entity.SiteStatus;
import scripting.data.entity.Status;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.TextUtility;
import scripting.util.captcha.CaptchaWork;
import scripting.util.concurrent.ManualResetEvent;
import scripting.util.http.NukeHttpClient;
import scripting.util.http.Proxy;

/**
 *
 * @author Phuc
 */
public abstract class TaskBase implements Runnable{
    
    // network object
    public NukeHttpClient client;
     
     // control variable
     protected ManualResetEvent stopEvent;
     protected final Status SUCCESS = Status.SUCESSS;
     protected final Status FAILURE = Status.FAILED;
     
     protected String siteName;
     protected int maxSteps = 20;
     private Proxy currentProxy;
     
     protected boolean isStopped;
     
     ExecutorService requestService;
     
     public String handleCaptcha(String url) throws InterruptedException, ExecutionException, IOException, TaskStoppedException, SubmissionTimeoutException {

        try {
            if (url.toLowerCase().contains("blogs.24.com")
                    || url.toLowerCase().contains("yourtrainings.com")
                    || url.toLowerCase().contains("sina.com.cn")
                    || url.toLowerCase().contains("rediff.com")
                    || url.toLowerCase().contains("bravejournal.com")
                    || url.toLowerCase().contains("mywapblog.com")
                    || url.toLowerCase().contains("blogymate.com")
                    || url.toLowerCase().contains("fizzlive.com")
                    || url.toLowerCase().contains("lib/plugins/captcha/img.php?secret=")//WikiDoku
                    || url.toLowerCase().contains("/includes/captcha.php")//phpmotion
                    || url.toLowerCase().contains("si-captcha-for-wordpress/captcha/securimage_show.php")//wordpress
                    || url.toLowerCase().contains("image_captcha/")//drupal
                    || url.toLowerCase().contains("/securimage/securimage_show.php")//article script
                    || url.toLowerCase().contains("/captcha/image/id_")//phpfox
                    || url.toLowerCase().contains("/pg/captcha/")//elgg
                    || (url.toLowerCase().contains("/captcha/") && url.split("/").length == 5)//elgg
                    || url.toLowerCase().contains("/simg/simg.php")//dolphin
                    || url.toLowerCase().contains("tiki-random_num_img.php")//WikiTiki
                    || url.toLowerCase().contains("includes/fns/fns.captcha.php")//WikiTiki
                    || url.toLowerCase().contains("www.google.com/recaptcha/api/image")//google captcha
                    ) {
                return handleCaptchaBreaker(url);
            }
            //client.disableProxy();
            InputStream rawImage = client.DownloadImage(url);
            CaptchaWork captchaWork = new CaptchaWork(rawImage, this.stopEvent);
            captchaWork.setExecutorService(requestService); // TODO late constructor request service
            captchaWork.process();
            String captcha = captchaWork.getCaptcha();
            System.out.println("\naaaa	" + "	" + url + "	" + captcha);
            return captcha;
        } finally {
            client.enableProxy();
        }
    }
     
     public String handleCaptchaBreaker(String url) throws InterruptedException, ExecutionException, IOException, TaskStoppedException, SubmissionTimeoutException {

        try {

            //client.disableProxy();
            InputStream rawImage = client.DownloadImage(url);
            CaptchaWork captchaWork = new CaptchaWork(rawImage, this.stopEvent);
            captchaWork.setExecutorService(requestService);
            captchaWork.process();
            String captcha = captchaWork.getCaptcha();
            System.out.println("\naaaabreaker	"  + "	" + url + "	" + captcha);
            return captcha;
        } finally {
            client.enableProxy();
        }
    }
     
    // wait some seconds
    public void wait(int timeOut) throws InterruptedException, TaskStoppedException {
        boolean waitResult = this.stopEvent.waitOne(timeOut, TimeUnit.SECONDS);
        if (waitResult == true) {
            throw new TaskStoppedException("Task Stopped");
        } else {
            this.stopEvent.reset();
        }
    }
    
    public ReCaptchaInfo getReCaptchaInfo(String key) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, TaskStoppedException, ExecutionException, IOException {
        try {
            String captchaKey = key.trim();// TextUtility.ExtractOne(client.getRawBody(), "challenge\\?k=(.*?)\"").trim();
            client.AddRequestHeader("Accept", "*/*");
            client.AddRequestHeader("Referer", client.getCurrentURL());
            String js = client.DownloadString("http://www.google.com/recaptcha/api/challenge?k=" + captchaKey);            
            if (!Strings.isNullOrEmpty(js)) {

                String captchaKey1 = TextUtility.ExtractOne(js, "challenge : '(.*?)'");
                String sitecaptcha = TextUtility.ExtractOne(js, "site : '(.*?)'");
                if (Strings.isNullOrEmpty(captchaKey1)|| Strings.isNullOrEmpty(sitecaptcha)){
                    System.out.println(captchaKey + "\n" + js);
                    return null;
                }
                
                String js1 = client.DownloadString("http://www.google.com/recaptcha/api/reload?c=" + captchaKey1 + "&k=" + sitecaptcha + "&reason=i&type=image");
                String captchaKey2 = TextUtility.ExtractOne(js1, "'(.*?)'");

                if (Strings.isNullOrEmpty(captchaKey2)){
                    return null;
                }
                
                String captchaURL = "https://www.google.com/recaptcha/api/image?c=" + captchaKey2;

                return new ReCaptchaInfo(captchaKey2, captchaURL);
            } else {
                return null;
            }
        } catch (Exception ex) {
            return null;
        } finally {
            client.RemoveRequestHeader("Accept");
        }
    }
    
    public boolean SolveGoogleCaptcha(String key) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, TaskStoppedException, ExecutionException, IOException {
        try {
            String captchaKey = TextUtility.ExtractOne(client.getRawBody(), "challenge\\?k=(.*?)\"");
            if (Strings.isNullOrEmpty(captchaKey)) {
                captchaKey = TextUtility.ExtractOne(client.getRawBody(), "Recaptcha.create\\('(.*?)'");
            }
            if (Strings.isNullOrEmpty(captchaKey)) {
                captchaKey = key;
            }
            if (Strings.isNullOrEmpty(captchaKey)) {
                return false;
            }
            client.AddRequestHeader("Accept", "*/*");
            client.AddRequestHeader("Referer", client.getCurrentURL());
            String js = client.DownloadString("http://www.google.com/recaptcha/api/challenge?k=" + captchaKey.trim());
            if (!Strings.isNullOrEmpty(js)) {

                String captchaKey1 = TextUtility.ExtractOne(js, "challenge : '(.*?)'");
                String sitecaptcha = TextUtility.ExtractOne(js, "site : '(.*?)'");
                
                
                if (Strings.isNullOrEmpty(captchaKey1)|| Strings.isNullOrEmpty(sitecaptcha)){
                    setWaitInfo("loi google captcha");
                    return false;                    
                }
                
                String js1 = client.DownloadString("http://www.google.com/recaptcha/api/reload?c=" + captchaKey1 + "&k=" + sitecaptcha + "&reason=i&type=image");
                String captchaKey2 = TextUtility.ExtractOne(js1, "'(.*?)'");
                
                if (Strings.isNullOrEmpty(captchaKey2)){
                    setWaitInfo("captchaKey2 null: google captcha");
                    return false;                    
                }
                
                client.AddPostParam("recaptcha_challenge_field", captchaKey2);
                String captchaURL = "https://www.google.com/recaptcha/api/image?c=" + captchaKey2;
                String captcha = this.handleCaptcha(captchaURL);
                client.AddPostParam("recaptcha_response_field", captcha);
                setWaitInfo(captcha + ":google captcha");
                return true;
            }
            return false;
        } catch (Exception ex) {
            System.out.println("\nsolving google captcha loi:\n" + client.getCurrentURL() + "\n" + ex.getMessage());
            return false;
        } finally {
            client.RemoveRequestHeader("Accept");
        }
    }
    
    public boolean SolveMediaCaptcha() throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, TaskStoppedException, ExecutionException, IOException {
        try {
            if (client.responseContains("adcopy_response")) {
                String str = client.DownloadString("http://api.solvemedia.com/papi/_challenge.js?k=NtL1K7.nkrFIlWOSHxnhs3Ghz8GSYpwO;f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,swf11,swf11.8,swf,h5c,h5ct,svg,h5v,v/h264,v/ogg,v/webm,h5a,a/mp3,a/ogg,ua/chrome,ua/chrome28,os/nt,os/nt5.1,fwv/Nn79ug.svqh69,jslib/jquery,jslib/modernizer;am=NhD0NHGFAJ4AbgrCcYUAng;ca=script;ts=1376491999;ct=1376492179;th=white;r=0.5583233307115734");
                String str1 = TextUtility.ExtractOne(str, "chid\"\\s*:\\s*\"(.*?)\"");
                String urlimg = "http://api.solvemedia.com/papi/media?c=" + str1 + ";w=300;h=150;fg=000000;bg=f8f8f8";
                client.AddPostParam("adcopy_challenge", str1);
                String captcha = handleCaptcha(urlimg);
                client.AddPostParam("adcopy_response", captcha);
                return true;
            }
            return false;
        } catch (Exception ex) {
            System.out.println("\nsolving media captcha loi:\n" + client.getCurrentURL() + "\n" + ex.getMessage());
            return false;
        } finally {
        }
    }

    public boolean SolveMediaCaptcha(String key) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, TaskStoppedException, ExecutionException, IOException {
        try {
            if (client.responseContains("adcopy_response") || !Strings.isNullOrEmpty(key)) {
                String tm = TextUtility.TimeStampSeconds();
                if (Strings.isNullOrEmpty(key)) {
                    key = "NtL1K7.nkrFIlWOSHxnhs3Ghz8GSYpwO";
                }
                String tm1 = TextUtility.TimeStampSeconds();
                client.AddRequestHeader("Accept", "*/*");
                String str = client.DownloadString("http://api.solvemedia.com/papi/_challenge.js?k=" + key + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,swf11,swf11.8,swf,h5c,h5ct,svg,h5v,v/h264,v/ogg,v/webm,h5a,a/mp3,a/ogg,ua/chrome,ua/chrome28,os/nt,os/nt5.1,fwv/Nn79ug.svqh69,jslib/jquery,jslib/modernizer;am=NhD0NHGFAJ4AbgrCcYUAng;ca=script;ts=" + tm + ";ct=" + tm1 + ";th=white;r=0.5583233307115734");
                String str1 = TextUtility.ExtractOne(str, "chid\"\\s*:\\s*\"(.*?)\"");
                String urlimg = "http://api.solvemedia.com/papi/media?c=" + str1 + ";w=300;h=150;fg=000000;bg=f8f8f8";
                client.AddPostParam("adcopy_challenge", str1);
                client.AddRequestHeader("Accept", "image/webp,*/*;q=0.8");
                String captcha = handleCaptcha(urlimg);
                client.AddPostParam("adcopy_response", captcha);
                return true;
            }
            return false;
        } catch (Exception ex) {
            System.out.println("\nsolving media captcha loi:\n" + client.getCurrentURL() + "\n" + ex.getMessage());
            return false;
        } finally {
        }
    }

    public ReCaptchaInfo getReCaptchaInfoGoogle(String key, String link) throws InterruptedException, SubmissionTimeoutException, TaskStoppedException, TaskStoppedException, ExecutionException, IOException {
        String keyInfoStr = this.client.DownloadString("https://www.google.com/recaptcha/api/challenge?k=" + key + "&ajax=1&" + link);
        if (keyInfoStr != null) {
            String challengeKey = TextUtility.ExtractOne(keyInfoStr, "challenge : '(.*?)'");//body.getElementById("recaptcha_challenge_field").attr("value");
            String captchaURL = "http://www.google.com/recaptcha/api/image?c=" + challengeKey;
            return new ReCaptchaInfo(challengeKey, captchaURL);
        } else {
            return null;
        }
    }
    
    public abstract void stop(String love);

    public String getTaskName() {
        return siteName;
    }
    
    public int InputMaxLength(String elementLength, int maxLength) {
        try {
            if (!Strings.isNullOrEmpty(elementLength)) {
                return Integer.valueOf(elementLength);
            } else {
                return maxLength;
            }
        } catch (Exception ex) {
            return maxLength;
        }
    }
    
    public int InputMaxLength(Element element, int maxLength) {
        try {
            if (element != null) {
                if (element.hasAttr("maxlength")) {
                    String max = element.attr("maxlength");
                    if (!Strings.isNullOrEmpty(max)) {
                        return Integer.valueOf(max);
                    } else {
                        return maxLength;
                    }
                }

            }
            return maxLength;
        } catch (Exception ex) {
            return maxLength;
        }
    }
    
    public void setTimeout(int timeout) {
        client.setTimeOut(timeout);
    }
    public boolean IsUserProxy;
     public TaskBase() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException {
        //this.parentWork = parentWork;
         
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("taskService-%d").build();
        factory = new ThreadFactoryBuilder().setNameFormat("requestService-%d").build();
        ExecutorService requestServiceDelegate = Executors.newFixedThreadPool(5, factory);
        requestService = MoreExecutors.listeningDecorator(requestServiceDelegate);
        stopEvent = new ManualResetEvent(false);
        client = new NukeHttpClient(true);
        client.setTimeOut(10000); // this is set time out
        //client.setRequestExecutorService(this.parentWork.requestService);
        client.setStopEvent(stopEvent);
        //if (IsUserProxy) {
        //    scripting.util.http.Proxy p = new scripting.util.http.Proxy("192.169.1.1", 1234, "username", "password");//=  MainApp.proxyMonitor.getRandomProxy();
        //    if (p != null) {
        //        this.setProxy(p);
//                HttpHost proxy = new HttpHost(p.getAddress(), p.getPort());
//                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//                if (Strings.isNullOrEmpty(p.getUsername()) == false) {
//                    client.getCredentialsProvider().setCredentials(new AuthScope(p.getAddress(), p.getPort()), new UsernamePasswordCredentials(p.getUsername(), p.getPassword()));
//                }
            //} else {
                // throw exception
            //}
        //}
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
        HttpConnectionParams.setSoTimeout(params, 10 * 1000);
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE; IEMobile 6.12; Microsoft ZuneHD 4.3)");
        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }
     
    public void setProxy() {
    }

    public void setProxy(Proxy p) {
        client.setProxy(p);
    }
    
    public void setWaitInfo(String waitInfo) {
        System.out.println("Status: " + waitInfo);
    }
    
    public String GenMonth01() {
        return TextUtility.GenMonth01();
    }

    public String GenMonth1() {
        return TextUtility.GenMonth1();
    }

    public String GenDay() {
        return TextUtility.GenDay();
    }

    public String GenYear() {
        return TextUtility.GenYear();
    }
    
    public void ReportCaptchaFail(){
        
    }
}
