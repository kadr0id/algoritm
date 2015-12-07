/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.nodes.Element;
import scripting.base.ACTaskBase;
import scripting.captcha.ReCaptchaInfo;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.TextUtility;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;


public class ACSOUP extends ACTaskBase {

    private String url;

    public ACSOUP() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }

    @Override
    public int runStep(int step) throws IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException {
        int result = -1;

        switch (step) {
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Loading registration page...");
                url = "http://" + site + "/signup";
                client.Navigate(url);

                waitText = "Username|A NEW USER";
                nextStep = 3;

                break;
            case 3:
                setWaitInfo("Filling in registration details ...");
                Element form = client.FindElement("form[action*=/signup");
                client.AddFixedPostParamForm(form);
                client.AddPostParam("user[login]", username());
                client.AddPostParam("user[password]", password());
                client.AddPostParam("user[email]", email());
                String authenticity_token = client.FindElement("input[name=authenticity_token").attr("value");
                String csrf_token = client.FindElement("meta[name=csrf-token").attr("content");
                client.AddRequestHeader("X-CSRF-Token", csrf_token);
                client.AddPostParam("authenticity_token", authenticity_token);
                client.AddRequestHeader("Referer", url);

                nextStep = 48;

                break;
            case 48:
                setWaitInfo("Solving captcha ...");
                String captchaKey = TextUtility.ExtractOne(client.getRawBody(), "challenge\\?k=(.*?)\"");
                super.SolveGoogleCaptcha(captchaKey);
                nextStep = 4;
                break;
            case 4:
                setWaitInfo("Submitting registration...");

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST(client.getCurrentURL());
                if (client.responseContains("Login has already been taken")) {
                    setWaitInfo("Login has already been taken.");
                    String newUserName = username() + TextUtility.GenerateRandomNum(2) + TextUtility.GenerateRandomChar(2);
                    setUsername(newUserName);
                }
                waitText = "Word verification response is incorrect|s soup";
                nextStep = 5;

                break;
            case 5:
                setWaitInfo("Account Created");
                result = 1;
                break;
        }

        return result;
    }
}
