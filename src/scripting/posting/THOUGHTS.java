package scripting.posting;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import scripting.base.SNTaskBase;
import scripting.util.TextUtility;

public class THOUGHTS extends SNTaskBase {

    private Element form;
    private final String embedYoutubeCode;
    private String postingURL;

    public THOUGHTS(String embedYoutubeCode) throws Throwable {
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
                result = login();
                break;

            case 3:
                result = navigateToPosting();
                break;
        }
        return result;
    }

    private int login() throws Exception {
        form = client.FindElement("form[action*=/users/sign_in");
        client.AddFixedPostParamForm(form);
        client.AddPostParam("utf8", "%E2%9C%93");
        String authenticity_token = client.FindElement("input[name=authenticity_token").attr("value");
        client.AddPostParam("authenticity_token", authenticity_token);
        client.AddPostParam("user[login]", username());
        client.AddPostParam("user[password]", password());
        client.AddPostParam("commit", "Sign in");
        String submiturl = client.getCurrentURL() + "/users/sign_in";
        client.POST(submiturl);
        if (client.responseContains("Invalid email or password.")) {
            setWaitInfo(String.format("Invalid email(%s) or password(%s).", username(), password()));
            return SkipSiteOrRetry();
        }
        if (client.responseContains("Write a new post:")) {
            setWaitInfo("Successfully logged in!");
            nextStep = 3;
        } else {
            setWaitInfo("Something went wrong.");
        }
        return -1;
    }

    private int navigateToPosting() throws Exception {
        int result;
        postingURL = String.format("http://%s.%s/post/video", username(), site);
        client.Navigate(postingURL);

        setWaitInfo("Link edit: " + client.getCurrentURL());
        form = client.FindElement("form[action=/post/videocreate]");
        if (form != null) {
            setWaitInfo("Posting form has been find!");
            result = posting();
        } else {
            setWaitInfo("Site can't post");
            result = 0;
        }
        return result;
    }

    private int posting() throws Exception {
        java.util.Date date = new java.util.Date();
        int year = date.getYear() + 1900;
        int month = date.getMonth() + 1;
        int day = date.getDay();
        int hours = date.getHours();
        int minutes = date.getMinutes();
        String postDate = year +"-"+ month +"-"+ day +" "+ hours +":"+ minutes +":00+0000";
        String urlSrc = Jsoup.parse(embedYoutubeCode).select("iframe").attr("src") + "\"";

        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        client.AddRequestHeader("Content-Type", "application/json; charset=utf-8");
        client.AddRequestHeader("Referer", postingURL);
        client.AddRequestHeader("Origin", String.format("http://%s.%s", username(), site));
        client.AddPostParam("utf8", "%E2%9C%93");
        client.AddHiddenParams("form[id=new_post]");
        form = client.FindElement("form[id=new_post]");
        client.AddFixedPostParamForm(form);

        String authenticity_token = client.FindElement("input[name=authenticity_token").attr("value");
        client.AddPostParam("authenticity_token", authenticity_token);

        String blog_token = client.FindElement("select[name=post[blog_id]] > option").attr("value");
        client.AddPostParam("post[blog_id]", blog_token);

        client.AddPostParam("post[title]", TextUtility.GenerateRandomChar(5));
        client.AddPostParam("post[url]", embedYoutubeCode);
        client.AddPostParam("post[body]", "<p>video</p>");
        client.AddPostParam("post[post_type]", "v");
        client.AddPostParam("post[topic_list]", "video");
        client.AddPostParam("post[privacy]", "o");
        client.AddPostParam("post[allow_repost]", "true");
        client.AddPostParam("post[publish_time]", postDate);
        client.AddPostParam("date[month]", String.valueOf(month));
        client.AddPostParam("date[day]", String.valueOf(day));
        client.AddPostParam("date[year]", String.valueOf(year));
        client.AddPostParam("date[hour]", String.valueOf(hours));
        client.AddPostParam("date[minute]", String.valueOf(minutes));
        client.AddPostParam("type", "html");
        client.AddPostParam("original_url", urlSrc);
        client.AddPostParam("url", urlSrc + "%22");
        client.AddPostParam("title", TextUtility.GenerateRandomChar(5));
        client.AddPostParam("favicon_url", "https://www.youtube.com/favicon.ico");
        client.AddPostParam("provider_url", "https://www.youtube.com/");
        client.AddPostParam("object_type", "video");
        client.AddPostParam("html", embedYoutubeCode);
        client.AddPostParam("image_url", "");


        String url = client.getCurrentURL().concat("create");
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
        client.AddRequestHeader("Upgrade-Insecure-Requests", "1");
        client.AddRequestHeader("Referer", siteName);
        client.AddRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
        client.Navigate(siteName);

        nextStep += 1;
    }
}
