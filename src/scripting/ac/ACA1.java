/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import org.apache.http.nio.reactor.IOReactorException;
import scripting.captcha.ReCaptchaInfo;
import scripting.base.ACTaskBase;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.TextUtility;

/**
 *
 * @author macbookpro
 */
public class ACA1 extends ACTaskBase {

    public ACA1() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }
    private String captchaKey;

    @Override
    public int runStep(int step) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException {
        int result = -1;
        //PHP LD
        switch (step) {
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Loading registration page...");
                client.Navigate("http://" + site + "/index.php?page=register");

                waitText = "New User Registration|First name|first_name";
                // = true;
                nextStep = 3;

                break;
            case 3:
                setWaitInfo("Filling in registration details ...");

                client.AddHiddenParamsContains("register");

                if (client.FindElement("input[name=first_name]") != null) {
                    client.AddPostParam("first_name", fName());
                }
                if (client.FindElement("input[name=last_name]") != null) {
                    client.AddPostParam("last_name", lName());
                }
                if (client.FindElement("input[name=email]") != null) {
                    client.AddPostParam("email", email());
                }
                if (client.FindElement("select[name=user_hear]") != null) {
                    client.AddPostParam("user_hear", String.valueOf(TextUtility.GenerateNumBetween(1, 7)));
                }
                if (client.FindElement("select[name=user_role]") != null) {
                    client.AddPostParam("user_role", fName());
                }
                if (client.FindElement("input[name=user_daily_email]") != null) {
                    client.AddPostParam("user_daily_email", "1");
                }
                if (client.FindElement("input[name=submitregister]") != null) {
                    client.AddPostParam("submitregister", "Register");
                }

                nextStep = 48;

                break;
            case 48:
                String captcha = "";
                String img = "";
                captchaKey = TextUtility.ExtractOne(client.getRawBody(), "challenge\\?k=(.*?)\"");
                if (captchaKey != null) {
                    ReCaptchaInfo captchaInfo = this.getReCaptchaInfo(captchaKey);
                    client.AddPostParam("recaptcha_challenge_field", captchaInfo.getChallengeKey());
                    captcha = this.handleCaptcha(captchaInfo.getCaptchaURL());
                    client.AddPostParam("recaptcha_response_field", captcha);

                } else if (client.responseContains("CaptchaSecurityImages.php?")) {
                    img = client.FindElement("img", "CaptchaSecurityImages.php?").attr("src");
                    captcha = this.handleCaptcha(img);
                    client.AddPostParam("security_code", captcha);

                } else if (client.responseContains("includes/img.php")) {
                    img = client.FindElement("img", "includes/img.php").attr("src");
                    captcha = this.handleCaptcha(img);
                    client.AddPostParam("code", captcha);

                } else if (client.responseContains("answer")) {
                    String p = TextUtility.ExtractOne(client.getRawBody(), " What is (.*?) =");
                    if (p != null) {
                        String sQuest = p.replace(" ", "");
                        String sAns = client.DownloadString("http://www.wolframalpha.com/input/instantmath.jsp?i=" + URLEncoder.encode(sQuest.toString(), "UTF-8"));
                        sAns = TextUtility.ExtractOne(sAns, "(\\s*-?\\s*\\d*\\.?\\d+)");
                        client.AddPostParam("answer", sAns);
                    }

                }
                nextStep = 4;
                break;
            case 4:
                setWaitInfo("Submitting registration...");

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST(client.getCurrentURL());
                waitText = "The Image Phrase entered is incorrect|Your registration is complete|Your registration is now complete|This email address is already|*message";
                // = true;
                nextStep = 5;

                break;
            case 5:
                setUsername(email());
                setWaitInfo("Account Created");
                result = 1;
                break;
        }

        return result;
    }
}
