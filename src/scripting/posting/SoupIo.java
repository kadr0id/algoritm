package scripting.posting;

import org.jsoup.nodes.Element;
import scripting.base.SNTaskBase;

public class SoupIo extends SNTaskBase {

    private Element form;
    private final String embedYoutubeCode;
    private String postingURL;

    public SoupIo(String embedYoutubeCode) throws Throwable {
        super();
        this.embedYoutubeCode = embedYoutubeCode;
    }

    @Override
    public int runStep(int step) throws Throwable {
        int result = -1;

        switch (step) {
            case 1:
                navigateToLogin();
                break;
            case 2:
                login();
                break;
            case 3:
                result = navigateToPosting(result);
                break;
        }
        return result;
    }

    private void login() throws Exception {
        form = client.FindElement("form[action*=/login");
        client.AddFixedPostParamForm(form);
        client.AddPostParam("login", username());
        client.AddPostParam("password", password());
        String submiturl = client.getCurrentURL();

        client.POST(submiturl);

        nextStep += 1;
    }

    private int navigateToPosting(int result) throws Exception {
        if (client.responseContains("Sorry, that login data doesn't match our records.")) {
            setWaitInfo("Login fail");
            result = SkipSiteOrRetry();
        } else if (client.responseContains("*pending")) {
            setWaitInfo("Wait for approved");
            Retry(2);
        } else {
            postingURL = String.format("http://%s.%s/new/video", username(), site.replaceAll("www.", ""));
            client.POST(postingURL);
            postingURL = String.format("http://%s.%s/", username(), site.replaceAll("www.", ""));
            client.Navigate(postingURL);

            setWaitInfo("Link edit: " + client.getCurrentURL());
            form = client.FindElement("form[action=/save]");
            if (form != null) {
                result = posting();
            } else {
                setWaitInfo("Site can't post");
                result = 0;
            }
        }
        return result;
    }

    private int posting() throws Exception {
        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        client.AddRequestHeader("Content-Type", "application/json; charset=utf-8");
        client.AddHiddenParams("form[id=new_post]");
        form = client.FindElement("form[id=new_post]");
        assert form != null;
        client.AddFixedPostParamForm(form);
        String authenticity_token = client.FindElement("input[name=authenticity_token").attr("value");
        String blog_token = client.FindElement("input[name=blog_token").attr("value");
        String csrf_token = client.FindElement("meta[name=csrf-token").attr("content");
        client.AddRequestHeader("X-BLOG-Token", blog_token);
        client.AddRequestHeader("X-CSRF-Token", csrf_token);
        client.AddRequestHeader("X-Prototype-Version", "1.7.1");
        client.AddPostParam("authenticity_token", authenticity_token);
        client.AddPostParam("post[embedcode_or_url]", embedYoutubeCode);

        client.AddPostParam("commit", "Save");
        client.AddPostParam("post[type]", "PostVideo");
        client.AddRequestHeader("Referer", postingURL);

        String url = postingURL + "save";
        client.POST(url);
        setWaitInfo("submitted: " + url);
        retryLeft = -1;
        isStopped = true;
        return 1;
    }

    private void navigateToLogin() throws Exception {
        client.ClearClientHeaders();
        setWaitInfo("Logging in...");
        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        client.AddRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
        client.Navigate(String.format("https://%s/login", site));

        nextStep += 1;
    }
}
