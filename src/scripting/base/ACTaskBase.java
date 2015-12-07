/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.base;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.data.entity.Status;
import scripting.util.TextUtility;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.Scripting;

/**
 *
 * @author GOD
 */
public abstract class ACTaskBase extends TaskBase {

    private String fName = "";
    private String lName = "";
    private String username = "";
    private String password = "";
    private String email = "";
    protected int nextStep = 1;
    protected int retryLeft = 0;
    protected String siteName;
    protected String waitText; // expected string for current request 
    protected String site;
    private int totalRetry;
    private String page = "";
    private Random rnd;
    
     public ListeningExecutorService requestService;
    public ExecutorService requestServiceDelegate;
     private final Object lock = new Object();
     public ExecutorService taskServiceDelegate;
    //private Random rnd;
    
    public void setMaxThread(int maxPermits) {
        synchronized (lock) {
            if (taskServiceDelegate != null) {
                ((ThreadPoolExecutor) taskServiceDelegate).setCorePoolSize(maxPermits);
                ((ThreadPoolExecutor) requestServiceDelegate).setCorePoolSize(maxPermits);
            }
        }
    }

    public ACTaskBase() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }

    public void Init(String siteName, String site, String fName, String lName, String username, String password, String email) {
       synchronized (lock) {
            ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("requestService-%d").build();
            requestServiceDelegate = Executors.newFixedThreadPool(5, factory);
            requestService = MoreExecutors.listeningDecorator(requestServiceDelegate);
        }
        this.client.setRequestExecutorService(requestService);
        this.fName = TextUtility.Spin(fName, ""); // {abc|xyx}
        this.lName = TextUtility.Spin(lName, "");
        this.username = TextUtility.Spin(username, "");
        this.password = TextUtility.Spin(password, "");
        this.email = email; 
        this.siteName = siteName;
        this.site = site;
        this.nextStep = 1;
        this.totalRetry = 4; // maximun number of retry 
        this.retryLeft = this.totalRetry;
        this.waitText = ""; 
        rnd = new Random();
    }

    //public void Init()
    public void Init(String fName, String lName, String username, String password, String email) throws IOReactorException {

        this.fName = fName;
        this.lName = lName;
        this.username = username;
        this.password = password;
        this.email = email;
        this.nextStep = 1;
        this.retryLeft = 4;
        waitText = "";
    }

    public void AutoRetry() {
        this.nextStep = 1;
        this.retryLeft--;
        waitText = "";
        scripting.util.http.Proxy p = null;
        if (p != null) {
            this.setProxy(p);
        }
    }

    public void Retry(int step) {
        this.nextStep = step;
        this.retryLeft--;
    }

    public void AddLog(String site, String message) {
    }

    @Override
    public void stop(String love) {
        this.stopEvent.setSource("cancelled");
        this.stopEvent.set();
        this.isStopped = true;
    }

    public abstract int runStep(int step) throws Throwable;

    @Override
    public void setTimeout(int timeout) {
        client.setTimeOut(timeout);
    }

    @Override
    public void run() {
        int result = -1;
        int step = 0;
        try {
            while (result == -1 && retryLeft > 0 && !isStopped) {
                client.setTimeOut(0);
                if (waitText != "") {
                    if (!client.responseContains(waitText)) {
                        AutoRetry();
                        continue;
                    }
                }
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    } else {
                        waitText = "";
                        result = runStep(nextStep);
                    }
                } catch (TaskStoppedException ex) {
                    //client.abort();
                    System.out.println(this.site);
                    result = -2;
                    break;
                } catch (InterruptedException ex) {
                    result = -2;
                    break;
                } catch (RejectedExecutionException ex) {
                    result = -2;
                    break;
                } catch (SubmissionTimeoutException ex) {
                    AutoRetry();
                } catch (Throwable ex) {

                    AutoRetry();
                    Logger.getLogger(ACTaskBase.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (result == 1) {
                System.out.println("Creat account successfully with (user, password, email): (" + username + "," + password + "," + email + ")");
            }
            if (isStopped) {
                result = -2;
            }
        } catch (Exception ex) {
            Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            client.shutdown();
        }
    }

    public void CheckForResult(String text) {
        if (client.responseContains(text) == false) {
            AutoRetry();
        }
    }

    public String getTaskName() {
        return this.siteName;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String email() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String fName() {
        return this.fName;
    }

    public String lName() {
        return this.lName;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPage() {
        return this.page;
    }

    public void setDeadAccount(boolean haspass) {
    }

    public String RandomSelectValue(String tagName, String attrName, String attrValue) {
        if (tagName.equals("select")) {
            Element selectNode = client.FindElement(String.format("select[%1$s=%2$s]", attrName, attrValue));
            if (selectNode != null) {
                Elements optionNodes = selectNode.select("option[value]");
                if (optionNodes.size() > 0) {
                    int rndInx = TextUtility.GenerateNumBetween(0, optionNodes.size() - 1);
                    return optionNodes.get(rndInx).attr("value");
                }

            }
        } else if (tagName.equals("radio")) {
            Elements radioNodes = client.FindElements(String.format("input[%1$s=%2$s]", attrName, attrValue));
            if (radioNodes.size() > 0) {
                int rndInx = rnd.nextInt(radioNodes.size() - 1) + 1;
                if (!Strings.isNullOrEmpty(radioNodes.get(rndInx).attr("value"))) {
                    return radioNodes.get(rndInx).attr("value");
                }

                for (int i = 0; i < radioNodes.size(); i++) {
                    Element radioNode = radioNodes.get(i);
                    if (!Strings.isNullOrEmpty(radioNode.attr("value"))) {
                        return radioNode.attr("value");
                    }

                }

            }
        }
        return "";
    }

    public String RandomSelectValue(String tag, String attr, String attrValue, String exceptText) {
        if (tag.equals("select")) {
            Element selectNode = client.FindElement(String.format("select[%1$s=%2$s]", attr, attrValue));
            if (selectNode != null) {
                Elements optionNodes = selectNode.select("option[value]");
//                for (int i = 1; i < optionNodes.size(); i++) {
//                    Element optionNode = optionNodes.get(i);
//                    String text = optionNode.text();
//                    text = text.trim();
//                    if (tags().toLowerCase().contains(text.toLowerCase()) && (!Strings.isNullOrEmpty(requiredText) && text.startsWith(requiredText))) {
//                        return optionNode.attr("value");
//                    }
//                }
                int count = 0;
                while (count < 10) {
                    count++;
                    int rndInx = rnd.nextInt(optionNodes.size());
                    Element optionNode = optionNodes.get(rndInx);
                    String selected = optionNode.attr("selected");
                    String text = optionNode.text().trim();
                    if (!Strings.isNullOrEmpty(selected)) {
                        if (!Strings.isNullOrEmpty(exceptText)) {
                            if (!text.startsWith(exceptText)) {
                                return optionNodes.get(rndInx).attr("value");
                            }
                        } else {
                            continue;
                        }

                    }

                    if (!Strings.isNullOrEmpty(exceptText)) {
                        if (!text.startsWith(exceptText)) {
                            return optionNodes.get(rndInx).attr("value");
                        }
                    } else {
                        return optionNodes.get(rndInx).attr("value");
                    }
                }
            }
        } else if (tag.equals("radio")) {
            Elements radioNodes = client.FindElements(String.format("input[%1$s=%2$s]", attr, attrValue));
            if (radioNodes.size() > 0) {
                int rndInx = rnd.nextInt(radioNodes.size() - 1) + 1;
                if (!Strings.isNullOrEmpty(radioNodes.get(rndInx).attr("value"))) {
                    return radioNodes.get(rndInx).attr("value");
                }

                for (int i = 0; i < radioNodes.size(); i++) {
                    Element radioNode = radioNodes.get(i);
                    if (!Strings.isNullOrEmpty(radioNode.attr("value"))) {
                        return radioNode.attr("value");
                    }

                }

            }
        }
        return "";
    }
}
