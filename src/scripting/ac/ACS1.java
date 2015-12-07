/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Tuan Fix 4/7 - 5/7/2014
 */
package scripting.ac;

import com.google.common.base.Strings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.base.ACTaskBase;
import scripting.util.TextUtility;
import scripting.util.http.RequestHelper;

// social blog php fox
public class ACS1 extends ACTaskBase {

    private Element form;
    private String captcha;
    private Element imgNode;
    private Element captchaNode;
    private String submiturl;
    private String wait = "";
    private int priretry = 0;
    private boolean multipost = false;

    public ACS1() throws Throwable {
        super();
    }

    @Override
    public int runStep(int step) throws Throwable {
        int result = -1;
        //Hiep  Blogs PHPFox  

        switch (step) {
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Signing up...");
                client.Navigate(String.format("http://%s/index.php?do=/user/register/", site));
//                waitText = "val[user_name]|val[email]|This Account Has Been Suspended";

                nextStep = nextStep + 1;

                break;
            case 2:
                if (client.responseContains("This Account Has Been Suspended")) {
                    result = 0;
                    setWaitInfo("This Account Has Been Suspended");
                    break;
                }
                setWaitInfo("Registering...");
                form = client.FindElement("form[action*=/user/register/]:has(input[name=val[email]])");
                if (form == null) {
                    form = client.FindElement("form[id=js_form]");
                }
                if (form == null) {
                    form = client.FindElement("form:has(input[name=val[email]])");
                }
                if (form == null) {
                    form = client.FindElement("form:has(input[name=val[full_name]])");
                }


                if (form == null) {
                    result = 0;
                    setWaitInfo("Null Form");
                    break;
                }
                client.AddFixedPostParamForm(form);
                Elements customNodes = form.select("input[name*=custom][type=text]");
                if (customNodes != null && customNodes.size() > 0) {
                    for (Element node : customNodes) {
                        client.AddPostParam(node.attr("name"), TextUtility.RandomFName() + " " + TextUtility.RandomLName());
                    }
                }

                if (client.FindElement("input[name=val[full_name]]") != null) {
                    client.AddPostParam("val[full_name]", fName() + " " + lName());
                }

                if (client.FindElement("input[name=val[user_name]]") != null) {
                    client.AddPostParam("val[user_name]", username());
                }

                if (client.FindElement("input[name=val[email]]") != null) {
                    client.AddPostParam("val[email]", email());
                }
                if (client.FindElement("input[name=val[confirm_email]]") != null) {
                    client.AddPostParam("val[confirm_email]", email());
                }

                if (client.FindElement("input[name=val[password]]") != null) {
                    client.AddPostParam("val[password]", password());
                }
                if (client.FindElement("input[name=val[spam][9]]") != null) {
                    client.AddPostParam("val[spam][9]", "kenya");
                }

                if (client.FindElement("input[name=val[spam][19]]") != null) {
                    client.AddPostParam("val[spam][19]", "5");
                }
                if (client.FindElement("input[name=val[password_confirm]]") != null) {
                    client.AddPostParam("val[password_confirm]", password());
                }

                Elements customnodes = form.select("input[name*=custom]");
                if (customnodes != null && customnodes.size() > 0) {
                    for (Element node : customnodes) {
                        client.AddPostParam(node.attr("name"), TextUtility.RandomFName());
                    }
                }

                client.RemovePostParam("val[parent_id]");
                client.RemovePostParam("val[item_id]");
                client.RemovePostParam("val[type]");
                if (client.FindElement("input[name=val[agree]]") != null) {
                    client.AddPostParam("val[agree]", "1");
                }
                if (client.FindElement("input[name=val[first_name]]") != null) {
                    client.AddPostParam("val[first_name]", fName());
                }
                if (client.FindElement("input[name=val[last_name]]") != null) {
                    client.AddPostParam("val[last_name]", lName());
                }

                if (client.FindElement("input[name=val[postal_code]") != null) {
                    client.AddPostParam("val[postal_code]", TextUtility.RandomZipcode());
                }
                Element countryNode = client.FindElement("input[name=custom[11]]");
                if (countryNode == null) {
                    countryNode = client.FindElement("input[name=custom[28]]");
                }


                if (countryNode != null) {
                    client.AddPostParam(countryNode.attr("name"), "NY");
                }

                Element phoneNode = client.FindElement("input[name=custom[26]]");
//                if (phoneNode==null){
//                    phoneNode = client.FindElement("input[name=custom[28]]");
//                }



                if (phoneNode != null) {
                    client.AddPostParam(phoneNode.attr("name"), TextUtility.GenerateRandomNum(9));
                }

                if (client.FindElement("input[type=file][name=image]") != null) {
                    client.AddFilePostParam("image", TextUtility.GetRandomAvatar());
                    multipost = true;
                }

                if (client.FindElement("input[name=custom[10]]") != null) {
                    client.AddPostParam("custom[10]", TextUtility.RandomAboutMe());
                }

                Elements textareaNodes = form.select("textarea[name]");
                if (textareaNodes != null && textareaNodes.size() > 0) {
                    for (Element node : textareaNodes) {
                        client.AddPostParam(node.attr("name"), TextUtility.RandomAboutMe());
                    }
                }
                if (priretry >= 1) {
                    nextStep = 48;
                    break;
                } else {
                    nextStep = 3;
                    break;
                }

            case 48:

                SolveGoogleCaptcha("");


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



                nextStep = 3;
                break;


            case 3:


                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                submiturl = client.getCurrentURL();
                if (form != null && form.hasAttr("action")) {
                    submiturl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }
                if (multipost == true) {
                    client.MultiPost(submiturl);
                } else {
                    client.POST(submiturl);
                }

                nextStep = nextStep + 1;

                break;
            case 4:
                if (client.responseContains("*verify|Logout|logout|email has been sent")) {
                    result = 1;
                    setWaitInfo("done");
                    break;
                }

                if (!client.getCurrentURL().contains("/register/")) {
                    if (client.FindElement("input[name=val[email]]") != null) {
                        setWaitInfo("OK:register/val[email]");
                        result = 1;
                        break;
                    }
                }

                if (priretry >= 1) {
                    Elements errorNodes = client.FindElements("div[class=error_message]");
                    String errorText = "";
                    if (errorNodes != null && errorNodes.size() > 0) {
                        for (Element erNode : errorNodes) {
                            errorText = errorText + "|" + erNode.text();
                        }
                        if (!Strings.isNullOrEmpty(errorText)) {
                            result = 0;
                            setWaitInfo(errorText);
                            break;
                        }

                        result = 1;
                        setWaitInfo("ok:error_message null");
                        break;
                    } else {
                        result = 1;
                        setWaitInfo("ok:error_message null");
                        break;
                    }
                } else {
                    nextStep = 2;
                    priretry++;
                    setWaitInfo("Retry 2");
                    break;
                }



        }

        return result;
    }
    String[] spamsQuestAns = {"The year after 2010 is|2011",
        "The year before 2010 is|2009",
        "Official currency of USA is|dollar",
        "What is the capital of England|london",
        "What is Capital City of Thailand|bangkok",
        "What is the Capital of India|Delhi",
        "¿Cuantas gomas tiene un automóvil?:|4",
        "Where do Sailors work|Juuban",
        "Who was India's first female Prime Minister|Indira Gandhi",
        "Welche Zahl folgt auf 10|11",
        "Type in the box: Az Kurdim|Az Kurdim",
        "What is blue color?|sky",
        "What colour is the car|red",
        "How many kittens in the picture|3",
        "What is the picture of|star",
        "Color of Clouds:|white"};
}
