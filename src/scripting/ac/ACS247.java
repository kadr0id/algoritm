 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.nodes.Element;
import scripting.captcha.ReCaptchaInfo;
import scripting.base.ACTaskBase;
import scripting.util.TextUtility;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.http.RequestHelper;

// @ Hiep - 2013/09/25
public class ACS247 extends ACTaskBase {

    private String captcha;
    private Element form;
    private String submiturl;
    private String wait;
    private String sit;

    public ACS247() throws Throwable {
        super();
    }

    @Override
    public int runStep(int step) throws Throwable {
        int result = -1;
        switch (step) {
            case 1:
                sit = site.replace("-hiep-", ".");
                setUsername(email().split("@")[0]);//
                client.ClearClientHeaders();
                setWaitInfo("Creating Account...");
                client.Navigate("http://www.bloguerama.com/?nav=subscribe&siteURL=www." + sit);
                waitText = "pseudo|siteURL";
                nextStep = nextStep + 1;
                break;
            case 2:
                form = client.FindElement("form[id=forminsc1]");

                if (form == null) {
                    form = client.FindElement("form[action*=index.php?nav=subscribe]");
                }
                client.AddFixedPostParamForm(form);
                client.AddPostParam("pseudo", username());

                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.AddRequestHeader("Referer", client.getCurrentURL());
                submiturl = "http://www.bloguerama.com/index.php?nav=subscribe";
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }
                client.POST(submiturl);
                waitText = "prenom|datenaissance[d]|passwd2|Esse login já se encontra registado|err=nick";
                if (client.responseContains("se encontra registado") || client.getCurrentURL().contains("err=nick")) {
                    setWaitInfo("Already exist.");
                    result = 0;
                } else {

                    nextStep = nextStep + 1;
                }
                break;

            case 3:

                form = client.FindElement("form[name=inscription]");
                if (form == null) {
                    form = client.FindElement("form[id=forminsc2]");
                }
                if (form == null) {
                    form = client.FindElement("form[action*=nav=subscribe-next&amp]");
                }
                client.AddFixedPostParamForm(form);
                client.AddPostParam("pseudo", username());
                client.AddPostParam("nom", username());
                client.AddPostParam("prenom", username());
                client.AddPostParam("email", email());
                client.AddPostParam("username", username());
                client.AddPostParam("passwd1", password());
                client.AddPostParam("passwd2", password());

                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.AddRequestHeader("Referer", client.getCurrentURL());
                submiturl = client.getCurrentURL();
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }
                client.POST(submiturl);

                waitText = "datenaissance[d]|passwd2|*activation&save=1|Activação do seu blog|activar a sua conta";
                nextStep = nextStep + 1;
                break;

            case 4:
                form = client.FindElement("form[name=inscription]");
                if (form == null) {
                    form = client.FindElement("form[id=forminsc2]");
                }
                if (form == null) {
                    form = client.FindElement("form[action*=nav=subscribe-next]");
                }
                client.AddFixedPostParamForm(form);
                client.AddPostParam("pseudo", username());
                client.AddPostParam("nom", username());
                client.AddPostParam("prenom", username());
                client.AddPostParam("email", email());
                //  client.AddPostParam("username", username());
                client.AddPostParam("passwd1", password());
                client.AddPostParam("passwd2", password());
                nextStep = 48;
            case 48:
                String captchaKey = TextUtility.ExtractOne(client.getRawBody(), "challenge\\?k=(.*?)\"");
                if (captchaKey != null) {
                    wait = "Solving google captcha";
                    setWaitInfo(wait);
                    ReCaptchaInfo captchaInfo = this.getReCaptchaInfo(captchaKey);
                    client.AddPostParam("recaptcha_challenge_field", captchaInfo.getChallengeKey());
                    captcha = this.handleCaptcha(captchaInfo.getCaptchaURL());
                    client.AddPostParam("recaptcha_response_field", captcha);
                    wait = captcha + ":google captcha";
                    setWaitInfo(wait);
                    nextStep = 5;
                    break;
                } else {
                    nextStep = 5;
                    break;
                }
            case 5:
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.AddRequestHeader("Referer", client.getCurrentURL());
                submiturl = client.getCurrentURL();
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }
                client.POST(submiturl);

                waitText = "*activation&save=1|Activação do seu blog|activar a sua conta";
                nextStep = nextStep + 1;
            case 6:
                if (client.responseContains("Activação do seu blog|activar a sua conta") || client.getCurrentURL().contains("activation&save=1")) {
                    setWaitInfo("Account Created");
                    result = 1;
                    break;

                } else {
                    setWaitInfo("Signup fail.");
                    result = 0;
                    break;
                }
        }
        return result;
    }
}
