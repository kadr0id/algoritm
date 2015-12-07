/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.posting;

import com.google.common.base.Strings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.base.SNTaskBase;
import scripting.util.TextUtility;
import scripting.util.http.RequestHelper;

//  PHP Fox
//@ Hiep
public class SNP1 extends SNTaskBase {

    private Element form;
    private String submiturl;
    private String sTitle;
    private String security_token;
    private Element imgNode;
    private String wait;
    private String captcha;
    private Element captchaNode;

    public SNP1() throws Throwable {
        super();
    }

    @Override
    public int runStep(int step) throws Throwable {
        int result = -1;

        switch (step) {
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Logging in...");
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.Navigate(String.format("http://%s/", site));
                if (client.responseContains("You have been banned")) {
                    result = 0;
                    setWaitInfo("banned");
                    break;
                }
                waitText = "val[login]|You have been banned";
                nextStep = nextStep + 1;
                break;
            case 2:

                form = client.FindElement("form[action*=/user/login");
                if (form == null) {
                    form = client.FindElement("form[id=js_login_form]");
                }
                if (form == null) {
                    form = client.FindElement("form:has(input[name=val[login]])");
                }
                if (form == null) {
                    form = client.FindElement("form:has(input[name=val[password]])");
                }

                client.AddFixedPostParamForm(form);
                client.AddPostParam("val[login]", email());
                client.AddPostParam("val[password]", password());

//                security_token = client.GetPostParam("phpfox[security_token]");
//                if (Strings.isNullOrEmpty(security_token)) {
//                    security_token = client.GetPostParam("core[security_token]");
//                }
                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                submiturl = client.getCurrentURL();
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                client.POST(submiturl);
                waitText = "Logout|Invalid Email|Invalid Password|Email Verification|logout|*verify|*pending";

                nextStep = nextStep + 1;

                break;
            case 3:
                if (client.responseContains("Invalid Email|Invalid Password")) {
                    setWaitInfo("Login fail");
                    result = SkipSiteOrRetry();
                } else if (client.responseContains("Email Verification|*verify")) {
                    setWaitInfo("Verification");
                    CheckActivate(false);
                    //result = SkipSiteOrRetry();
                    nextStep = 13;
                } else if (client.responseContains("*pending")) {
                    setWaitInfo("Wait for approved");
//                    CheckActivate(false);
//                    result = SkipSiteOrRetry();
                    Retry(2);
                } else {

                    client.Navigate(String.format("http://%s/index.php?do=/blog/add/", site));

                    if (client.responseContains("val[title]|val[text]|Add a New Blog")) {
                        setWaitInfo("Link edit: " + client.getCurrentURL());
                        nextStep = 4;
                        break;
                    }

                    client.Navigate(String.format("http://%s/event/add/", site));
                    form = client.FindElement("form:has(input[name=val[title]])");
                    if (form != null) {
                        nextStep = 4;
                        break;
                    }
                    
                    client.Navigate(String.format("http://%s/blognews/add/", site));
                    form = client.FindElement("form:has(input[name=val[title]])");
                    if (form != null) {
                        nextStep = 4;
                        break;
                    }
                    
                    
                    
                    

                    setWaitInfo("Site can't post");
                    result = 0;

                }

                break;

            case 13:

                Element resent = client.FindElement("input[value=Resend Verification Email]");
                if (resent == null) {
                    resent = client.FindElement("input[onclick*=user.verifySendEmail]");
                }
                if (resent != null) {
                    Element securitytoken = client.FindElement("input[name*=[security_token]]");
                    String prefix = "phpfox";
                    String secuName = "";
                    String secuValue = "";
                    if (securitytoken != null) {
                        secuName = securitytoken.attr("name");
                        secuValue = securitytoken.attr("value");
                        prefix = secuName.replace("[security_token]", "");
                        client.AddPostParam(secuName, secuValue);
                    }
                    String iUser = TextUtility.ExtractOne(resent.outerHtml(), "iUser=(.*?)'");
                    client.AddPostParam(prefix + "[ajax]", "true");
                    client.AddPostParam(prefix + "[call]", "user.verifySendEmail");
                    client.AddPostParam("iUser", iUser);
                    client.AddPostParam(prefix + "[is_admincp]", "0");

                    client.AddRequestHeader("Accept", "text/javascript, application/javascript, */*");
                    client.AddRequestHeader("Referer", client.getCurrentURL());
                    client.AddRequestHeader("X-Requested-With", "XMLHttpRequest");

                    client.POST(String.format("http://%s/static/ajax.php", site));

                    String call = TextUtility.ExtractOne(client.getRawBody(), "ajaxBox\\('(.*?)'");
                    String heigh = TextUtility.ExtractOne(client.getRawBody(), "height=(.*?)&width");
                    String width = TextUtility.ExtractOne(client.getRawBody(), "width=(.*?)&");
                    String message = TextUtility.ExtractOne(client.getRawBody(), "encodeURIComponent\\('(.*?)'");

                    client.AddPostParam(prefix + "[ajax]", "true");
                    client.AddPostParam(prefix + "[call]", call);
                    client.AddPostParam("heigh", heigh);
                    client.AddPostParam("width", width);
                    client.AddPostParam("message", message);
                    client.AddPostParam(secuName, secuValue);

                    client.AddRequestHeader("Accept", "text/html, */*");
                    client.POST(String.format("http://%s/static/ajax.php", site));
                    if (client.responseContains("Verification email sent")) {
                        setWaitInfo("resent active mail");
                    } else {
                        setWaitInfo("Fail resent active mail");
                    }
                } else {
                    setWaitInfo("not found resent active mail");
                }
                result = 0;
                break;
            case 4:
                sTitle = Spin(title()) + getRandomTitle();
                //http://www.amateurmagazines.com/index.php?do=/blog/66593/how-exactly-to-play-the-game/action

                form = client.FindElement("form[action*=blog/add/]");
                if (form == null) {
                    form = client.FindElement("form:has(input[name=val[title]])");
                }
                if (form == null) {
                    form = client.FindElement("form:has(textarea[name=val[text]])");
                }

                client.AddFixedPostParamForm(form);
                client.AddPostParam("val[title]", sTitle);
                String sBody = "";//TextUtility.ModifyAnchor(body());
                sBody ="<p>" +  body() + "</p>";
                
                if (client.FindElement("textarea[name=val[description]]")!=null){
                    client.AddPostParam("val[description]", body());
                }
                
                if (client.FindElement("textarea[name=val[text]]")!=null){
                    client.AddPostParam("val[text]", body());
                }
                
                if (client.FindElement("textarea[name=val[emails]]")!=null){
                    client.AddPostParam("val[emails]", "");
                }
                
                if (client.FindElement("textarea[name=val[personal_message]]")!=null){
                    client.AddPostParam("val[personal_message]", "");
                }
                
                if (client.FindElement("input[name=val[location]]")!=null){
                    client.AddPostParam("val[location]", TextUtility.RandomStateFullName());
                }

                if (client.responseContains("rel=\"3\" >Only Me</a></li>")) {
                    client.AddPostParam("val[privacy_comment]", "3");

                }
                
                

                Element privacyNode = client.FindElement("select[name=val[privacy]]");
                if (privacyNode != null) {
                    Elements optionNodes = privacyNode.select("option[value]");
                    if (optionNodes != null && optionNodes.size() > 0) {
                        for (Element node : optionNodes) {
                            if (node.text().toLowerCase().contains("public")) {
                                client.AddPostParam(privacyNode.attr("name"), node.attr("value"));
                                break;

                            }
                        }
                    }
                }
                client.RemovePostParam("val[draft]");
                client.RemovePostParam("val[preview]");
                client.RemovePostParam("val[post_status]");

                nextStep = 48;
                break;
            case 48:
                imgNode = client.FindElement("img[src*=/captcha/image/id");
                if (imgNode == null) {
                    imgNode = client.FindElement("img[src*=CaptchaSecurityImages.php]");
                }

                captchaNode = client.FindElement("input[name=val[image_verification]]");
                if (captchaNode == null) {
                    captchaNode = client.FindElement("input[name=security_code]");
                }
                if (imgNode != null && captchaNode != null) {
                    setWaitInfo("captcha thuong");
                    captcha = this.handleCaptcha(RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), imgNode.attr("src")));
                    client.AddPostParam(captchaNode.attr("name"), captcha);
                    wait = captcha + ":captcha thuong";

                }
                String sQuest = "";
                captchaNode = client.FindElement("input[name=val[antibot]]");
                if (captchaNode == null) {
                    captchaNode = client.FindElement("input[name=sfvalue1]");
                }
                if (captchaNode == null) {
                    captchaNode = client.FindElement("input[name*=val[spam]]");
                }

                if (captchaNode != null) {
                    Element parentNode = captchaNode;
                    for (int i = 0; i < 4; i++) {
                        sQuest = parentNode.text();
                        if (!Strings.isNullOrEmpty(sQuest)) {
                            break;
                        }
                        parentNode = parentNode.parent();
                    }
                    if (!Strings.isNullOrEmpty(sQuest)) {
                        String sAns = TextUtility.SolveMathCaptcha(sQuest);
                        if (Strings.isNullOrEmpty(sAns)) {
                            sAns = TextUtility.SolveQuestyCaptcha(sQuest, site);
                        }
                        if (!Strings.isNullOrEmpty(sAns)) {
                            client.AddPostParam(captchaNode.attr("name"), sAns);
                        } else {
                            setWaitInfo(sQuest);
                        }
                    }
                }


                captchaNode = client.FindElement("input[name=val[nmspam_answer]]");
                if (captchaNode != null) {
                    Element parentNode = captchaNode;
                    for (int i = 0; i < 4; i++) {
                        sQuest = parentNode.text();
                        if (!Strings.isNullOrEmpty(sQuest)) {
                            break;
                        }
                        parentNode = parentNode.parent();
                    }
                    if (!Strings.isNullOrEmpty(sQuest)) {
                        String sAns = TextUtility.SolveMathCaptcha(sQuest);
                        if (Strings.isNullOrEmpty(sAns)) {
                            sAns = TextUtility.SolveQuestyCaptcha(sQuest, site);
                        }
                        if (!Strings.isNullOrEmpty(sAns)) {
                            client.AddPostParam(captchaNode.attr("name"), sAns);
                        } else {
                            setWaitInfo(sQuest);
                        }
                    }
                }

                nextStep = 5;
                break;
            case 5:
                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                submiturl = client.getCurrentURL();
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }
                client.POST(submiturl);
                setWaitInfo("submitted: " + submiturl);

                nextStep = nextStep + 1;
                break;


            case 6:
                if (client.responseContains("This blog is pending an Admins approval.")) {
                    result = 0;
                    setWaitInfo("wait for approval");
                    break;
                }
                String url = client.getCurrentURL();
                if (checkURL(url)) {
                    setURL(url);
                    result = 1;
                    setWaitInfo("done");

                } else {
                    result = 0;
                    setWaitInfo("Fail:" + url);
                }

                break;
        }
        return result;
    }
}
