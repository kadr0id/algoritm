/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.base;

import com.google.common.base.Strings;
import org.apache.http.nio.reactor.IOReactorException;
import org.joda.time.DateTime;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.data.entity.*;
import scripting.data.entity.SocialNetworkProject;
import scripting.util.TextUtility;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.http.NukeHttpClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GOD
 */
public abstract class SNTaskBase extends TaskBase {

    protected String fName = "";
    protected String lName = "";
    protected int nextStep = 0;
    protected int retryLeft = 0;
    protected SocialNetworkProject project;
    //protected SocialNetwork work;  
    protected String waitText;
    protected String site;
    protected int currentAccountID;
    protected int nAccounts;
    protected List<AccountItem> accounts;
    private String url = "";
    protected String rssurl = "";
    protected String repostDomain = "";
    protected List<String> blogDomain;
    private String categoryName;
    private String categoryValue;
    private String linksOnPage;
    private int totalRetry;
    private int stepPassed = 0;
    private Random rnd;
    private String randomTitle = " ";

    public SNTaskBase() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }

    /*
     * public void setProject(ACProject project) { this.parentProject = project;
     * }
     */
    public void Init(String siteName, ProjectType group, List<AccountItem> items, SiteStatus status, SocialNetworkProject project, String site) {
        this.client.setRequestExecutorService(requestService);
        this.project = project;
        this.accounts = items;
        this.nAccounts = this.accounts.size();
        this.currentAccountID = 0;
        this.siteName = siteName;
        this.nextStep = 1;
        this.totalRetry = 3;
        this.retryLeft = this.totalRetry;
        this.waitText = "";
        this.site = site;
        this.blogDomain = project.getRotatedDomains();
        try {
            this.categoryValue = status.getCategoryValue();
            this.categoryName = "CategoryName";
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        rnd = new Random();
    }

    public String getLinkOnPage() {
        return this.linksOnPage;
    }

    public String getRandomTitle() {
        return "";
    }

    public void AutoRetry() {
        this.nextStep = 1;
        this.retryLeft--;
        waitText = "";
        this.stepPassed = 0;
    }

    public void Retry(int step) {
        this.nextStep = step;
        this.retryLeft--;
        waitText = "";
    }

    public int SkipSiteOrRetry() {
        this.currentAccountID++;
        this.AutoRetry();
        if (this.currentAccountID >= this.nAccounts) {
            return 0;
        } else {
            return -1;
        }
    }

    public void CheckActivate(boolean active) {
    }

    public void AddLog(String site, String message) {
    }

    public void AddLog(String site, String title, String message) {
    }

    public String username() {
        return this.accounts.get(currentAccountID).getUsername();
    }

    public String password() {
        return this.accounts.get(currentAccountID).getPassword();
    }

    public String body() {
        String spunBody = Spin(project.getBody()).replace("<a href=\\\"http", "<a href=\"http").replace("\\\">", "\">").replace("\\n", "\r");
        linksOnPage = TextUtility.GetURLsOnPage(spunBody);
        return spunBody;
    }

    public String tags() {
        return project.getTags();
    }

    public String title() {
        return project.getTitle();
    }

    public String domain() {
        return project.getDomain();
    }

    public boolean createNewDomain() {
        return project.isCreateNewDomain();
    }

    public void setSiteURL(String siteURL) {
    }

    public String Spin(String original) {
        //return TextUtility.Spin(original, "");
        return this.Spin(original, "");
    }

    public String Spin(String original, String site) {
        String spunString = TextUtility.Spin(original, site);
//        if (original.equals(this.body())) {
//            linksOnPage = TextUtility.GetURLsOnPage(spunString);
//        }        
        return spunString;
    }

    public String catValue() {
        return this.categoryValue;
        //return this.status.getCategoryID();
    }

    public String catName() {
        return this.categoryName;
    }
    //public String catValue() {
    //}

    @Override
    public void stop(String love) {
        this.stopEvent.setSource("cancelled");
        this.stopEvent.set();
        //this.client.stop();
        this.isStopped = true;
        //this.sourceStopEvent.unregister(this);
    }

    public abstract int runStep(int step) throws Throwable;

    @Override
    public void run() {
        int result = -1;
        currentAccountID = -1;
        try {
            // for loop all account
            while(accounts.size() > 0 && currentAccountID < this.nAccounts){
                currentAccountID++;
                result = -1;
                AutoRetry();
                this.retryLeft = this.totalRetry;
                while (result == -1 && retryLeft > -1 && !isStopped) {
                    if (accounts.size() > 0 && currentAccountID >= this.nAccounts) {
                        break;
                    }
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
                            stepPassed++;
                            if (stepPassed > maxSteps) {
                                AutoRetry();
                            } else {
                                result = runStep(nextStep);
                            }
                        }
                    } catch (TaskStoppedException ex) {
                        result = -2;
                        break;
                    } catch (InterruptedException ex) {
                        result = -2;
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        break;

                    } catch (SubmissionTimeoutException ex) {
                        AutoRetry();
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (Throwable ex) {
                        AutoRetry();
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (result == 1) {
                System.out.println("Article Posted ...");
            }
        } finally {
            client.shutdown();
        }
    }

    public void CheckForResult(String text) {
        if (client.responseContains(text) == false) {
            AutoRetry();
        }
    }

    public void deleteAccount() {
//        AccountItem item = this.accounts.get(this.currentAccountID);
//        MainApp.dataService.deleteAccount(item);
    }

    public void setRepostDomain(String domain) {
        this.repostDomain = domain;
    }

    public List<String> getBlogDomain() {
        return this.blogDomain;
    }

    public String email() {
        return this.accounts.get(currentAccountID).getEmail();
    }

    public void setURL(String url) throws IOException {
        AddURL(3, url, siteName);
    }

    public void AddURL(int boxID, String url, String siteName) {
        if (url.contains("**")) {
            return;
        }
        switch (boxID) {
            case 1:
            case 3:
            case 5:
                /*URLItem item = new URLItem(url, "", "", urlListID, new Date(), projectType, siteName);
                if (!Strings.isNullOrEmpty(linksOnPage)) {
                    item.setLinksOnPage(linksOnPage);
                }
                if (Strings.isNullOrEmpty(project.getAnchor())) {
                    item.setAnchor(project.getTitle());
                } else {
                    item.setAnchor(project.getAnchor());
                }*/
                
                System.out.println("Backlink here: " + url);

                try {
                    this.url = url;
                    String fileOut = "AccountExport\\account_" + "export" + ".txt";
                    TextUtility.writeToFile(site + "," + username() + "," + password() + "," + email() + "\n", fileOut);
                } catch (Exception ex) {
                }

                break;
            case 2:
            case 4:
            case 6:
                break;
        }
    }

    public void setURL(String url, String idUrl) {
        AddURL(3, url, idUrl, username(), password(), ProjectType.SocialNetwork, siteName);
        this.url = url;
    }

    public void setURL(String url, String idUrl, String user, String pass, ProjectType projectType, String siteName) {
        AddURL(3, url, idUrl, user, pass, projectType, siteName);
        this.url = url;
    }

    public void setTime(DateTime dateTime) {
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    public String url() {
        return this.url;
    }

    public void AddURL(int boxID, String url, String anchorText, String idUrl) {
        if (url.contains("**")) {
            return;
        }
        switch (boxID) {
            case 1:
            case 3:
            case 5:
                /*URLItem item = new URLItem(url, "", "", urlListID, new Date(), idUrl);
                if (!Strings.isNullOrEmpty(linksOnPage)) {
                    item.setLinksOnPage(linksOnPage);
                }
                if (Strings.isNullOrEmpty(project.getAnchor())) {
                    item.setAnchor(project.getTitle());
                } else {
                    item.setAnchor(project.getAnchor());
                }
                MainApp.dataService.saveURL(item);*/
                System.out.println("Back link: " + url);
                try {
                    this.url = url;
                    String fileOut = "AccountExport\\account_" + "_exported" + ".txt";
                    TextUtility.writeToFile(site + "," + username() + "," + password() + "," + email() + "\n", fileOut);
                } catch (Exception ex) {
                }
                break;
            case 2:
            case 4:
            case 6:
                break;
        }
    }

    public void AddURL(int boxID, String url) {
        this.AddURL(boxID, url, "");
    }

    public void AddURL(int boxID, String url, String idUrl, String user, String pass, ProjectType projectType, String siteName) {
        if (url.contains("**")) {
            return;
        }
        switch (boxID) {
            case 1:
            case 3:
            case 5:
                /*URLItem item = new URLItem(url, "", "", urlListID, new Date(), idUrl, user, pass, projectType, siteName);
                if (!Strings.isNullOrEmpty(linksOnPage)) {
                    item.setLinksOnPage(linksOnPage);
                }
                if (Strings.isNullOrEmpty(project.getAnchor())) {
                    item.setAnchor(project.getTitle());
                } else {
                    item.setAnchor(project.getAnchor());
                }
                MainApp.dataService.saveURL(item);*/
                System.out.println("Back link: " + url);
                try {
                    this.url = url;
                    String fileOut = "AccountExport\\account_" + "_exported" + ".txt";
                    TextUtility.writeToFile(site + "," + username() + "," + password() + "," + email() + "\n", fileOut);
                } catch (Exception ex) {
                }
                break;
            case 2:
            case 4:
            case 6:
                break;
        }
    }

    public String RandomSiteCatValue(String tag, String attr, String attrValue) {
        return RandomSiteCatValue(tag, attr, attrValue, "");
    }

    public String RandomSiteCatValue(String tag, String attr, String attrValue, String requiredText) {
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
                    if (!Strings.isNullOrEmpty(selected)) {
                        continue;
                    }
                    String text = optionNode.text();
                    if (!Strings.isNullOrEmpty(requiredText)) {
                        if (text.startsWith(requiredText)) {
                            return optionNodes.get(rndInx).attr("value");
                        }
                    } else {
                        return optionNodes.get(rndInx).attr("value");
                    }
                }
            }
        } else if (tag.equals("input")) {
            Elements radioNodes = client.FindElements(String.format("input[%1$s=%2$s]", attr, attrValue));
            if (radioNodes.size() > 0) {
                int rndInx = rnd.nextInt(radioNodes.size());
                return radioNodes.get(rndInx).attr("value");
            }
        }
        return "";
    }

    public void SetPage(String url) {
        AccountItem item = this.accounts.get(this.currentAccountID);
        item.setPage(url);
    }

    public String GetPage() {
        AccountItem item = this.accounts.get(this.currentAccountID);
        return item.getPage();
    }

    protected boolean checkURL(String url) throws InterruptedException, TaskStoppedException, NoSuchAlgorithmException, KeyManagementException, Exception {
        wait(4);
        if (!Strings.isNullOrEmpty(this.linksOnPage)) {
            for (String urlonpage : this.linksOnPage.split("\\|")) {
                if (url.equalsIgnoreCase(urlonpage)) {
                    return false;
                }
            }
        }

        String[] tmp = linksOnPage.split("\\|");
        if (tmp.length == 0) {
            return false;
        }
        String[] links = new String[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            links[i] = tmp[i].toLowerCase();
        }

        NukeHttpClient client1 = new NukeHttpClient(true);

        client1.setRequestExecutorService(Executors.newFixedThreadPool(1));

        client1.setStopEvent(stopEvent);
        client1.enableProxy();
        for (int tryCount = 0; tryCount < 3; tryCount++) {
            try {
                boolean found = false;
                client1.ClearCookies();
                client1.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                String result = client1.DownloadString(url);
                result = result.toLowerCase();
                for (int linkID = 0; linkID < links.length; linkID++) {
                    if (result.contains(links[linkID])) {
                        found = true;
                        break;
                    }
                    String encodedLink = links[linkID].replace("&amp;", "&");
                    encodedLink = encodedLink.replace("&", "&amp;");
                    if (result.contains(encodedLink)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    return true;
                } else {
                    return false;
                }

            } catch (Exception ex) {
                throw ex;
            } finally {
                client1.getConnectionManager().shutdown();
                client1.getRequestExecutorService().shutdown();

            }
        }
        return false;
    }

    public String RandomSelectValueExceptText(String tag, String attr, String attrValue, String exceptText) {
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
