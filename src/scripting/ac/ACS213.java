/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.nodes.Element;
import scripting.base.ACTaskBase;
import scripting.util.TextUtility;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.http.RequestHelper;

/**
 *
 * @author Thinh
 */
public class ACS213 extends ACTaskBase {

    private Element form;
    private String randomUsername;

    public ACS213() throws Throwable {
        super();
    }

    public String getAuth(String s, int n) throws Exception {
        String s1 = URLDecoder.decode(s.substring(0, n) + s.substring(n + 1, s.length()), "UTF-8");
        String t = "";
        for (int i = 0; i < s1.length(); i++) {
            String tmp = Character.toString(s.charAt(n));
            t = t + Character.toString((char) ((int) (s1.charAt(i)) - Integer.valueOf(tmp)));
        }
        return URLDecoder.decode(t, "UTF-8");
    }

    @Override
    public int runStep(int step) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException, Exception {
        int result = -1;

        switch (step) {
            case 1:
                client.ClearClientHeaders();
                client.ClearCookies();                
                setWaitInfo("Creating Account...");
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.Navigate("http://www.sosblogs.com/en/create-your-blog/");
                waitText = "submit_theme_form|skin";
                nextStep = nextStep + 1;
                break;
            case 2:
                form = client.FindElement("form:has(input[name=submit_theme_form])");
                if (form == null) {
                    form = client.FindElement("form:has(input[name=skin])");
                }
                client.AddFixedPostParamForm(form);
                client.RemovePostParam("submit_form2");
                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.POST("http://www.sosblogs.com/en/create-your-blog/");
                waitText = "email|description";
                nextStep = 3;
                break;
            case 3:
                form = client.FindElement("form[action=/en/create-your-blog/");
                if (form == null) {
                    form = client.FindElement("form:has(input[name=user[password]])");
                }
                client.AddFixedPostParamForm(form);
                randomUsername = fName() + TextUtility.GenerateRandomNum(3) + TextUtility.GenerateRandomChar(3);
                client.AddPostParam("url", randomUsername);
                client.AddPostParam("domain", site);
                String f = email().split("@")[0];
                f = f + "+" + TextUtility.GenerateRandomChar(5);
                f = f + "@" + email().split("@")[1];
                
                client.AddPostParam("email", f);
                setPassword(password() + TextUtility.GenerateRandomNum(2));
                client.AddPostParam("password", password());

                client.AddPostParam("title", TextUtility.RandomFName() + " " + TextUtility.RandomLName());
                client.AddPostParam("description", TextUtility.RandomFName() + " " + TextUtility.RandomLName() + TextUtility.RandomFName() + " " + TextUtility.RandomLName());
                client.AddPostParam("rules", "on");
                nextStep = 48;
            case 48:
                Element imgNode = client.FindElement("img[src*=antirobotic.php]");
                Element captchaNode = client.FindElement("input[name=captcha]");
                if (imgNode != null && captchaNode != null) {
                    String imgurl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), imgNode.attr("src"));
                    String captcha = this.handleCaptcha(imgurl);
                    client.AddPostParam(captchaNode.attr("name"), captcha);
                } else {
                    Retry(1);
                    break;
                }
                nextStep = 7;
                break;
            case 7:
                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST("http://www.sosblogs.com/en/create-your-blog/");
                waitText = "confirm_password|This e-mail address is already used|already taken|already used|The password must contain at least 6 characters";
                nextStep = 4;
                break;
            case 4:
                if (client.responseContains("The password must contain at least 6 characters")) {
                    setWaitInfo("The password must contain at least 6 characters(letters and numbers)");
                    result = 0;
                    break;
                }
                if (client.responseContains("This e-mail address is already used|already taken|already used|The pass")) {
                    nextStep = 5;
                    break;
                }
                form = client.FindElement("form[name=form_register");
                if (form == null) {
                    form = client.FindElement("form:has(input[name=confirm_password])");
                }
                client.AddFixedPostParamForm(form);

                String authsall = TextUtility.ExtractOne(client.getRawBody(), "auth\\[\\]':\\[\\['(.*?)\\]\\]");
                if (authsall != null) {
                    String[] auths = authsall.split("\\],\\['");

                    String sauth1 = auths[0].split("',")[0];
                    int n1 = Integer.valueOf(auths[0].split("',")[1]);
                    String auth1 = getAuth(sauth1, n1);

                    String sauth2 = auths[1].split("',")[0];
                    int n2 = Integer.valueOf(auths[1].split("',")[1]);
                    String auth2 = getAuth(sauth2, n2);

                    client.AddPostParam("auth[]", auth1 + "|x|" + auth2);
                }

                client.AddPostParam("confirm_password", password());
                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST("http://www.sosblogs.com/en/create-your-blog/");
                waitText = "Your blog has been created";
                nextStep = nextStep + 1;
                break;
            case 5:
                if (client.responseContains("This e-mail address is already used|already taken|already used")) {
                    setWaitInfo("This e-mail address is already used|already used");
                    result = 0;
                } else {
                    String name_admin = randomUsername + "." + site;
                    setUsername(name_admin);
                    setWaitInfo("Account Created!");
                    result = 1;
                }
                break;
        }

        return result;
    }
}
