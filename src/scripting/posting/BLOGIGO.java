package scripting.posting;

import org.jsoup.nodes.Element;
import scripting.base.SNTaskBase;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.TextUtility;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;

public class BLOGIGO extends SNTaskBase {

    private Element form;
    private final String embedYoutubeCode;
    private String postingURL;

    public BLOGIGO(String embedYoutubeCode) throws Throwable {
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
                result = navigateToPosting();
                break;
        }
        return result;
    }

    private void login() throws Exception {
        form = client.FindElement("form[action*=/login/");
        client.AddFixedPostParamForm(form);
        client.AddPostParam("login_user", username());
        client.AddPostParam("login_pass", password());
        String submiturl = client.getCurrentURL();

        client.POST(submiturl);
        waitText = "My blogigo";
        nextStep += 1;
    }

    private int navigateToPosting() throws Exception {
        Element blogLink = client.FindElement("ul[id=myBlogs] > li > a");
        String blog_token = null;
        if (blogLink != null) {
            blog_token = blogLink.attr("href");
        }else {
            setWaitInfo("No blog was found");
            createNewBlog();
        }
        postingURL = siteName + blog_token + "editEntry";
        client.Navigate(postingURL);

        setWaitInfo("Link edit: " + postingURL);
        form = client.FindElement("form[name=edit_entry]");
        if (form != null) {
             return posting();
        } else {
            setWaitInfo("Site can't post");
            return  0;
        }
    }

    private void createNewBlog() throws Exception{
        String createURL = siteName + "/login/createBlog";
        client.Navigate(createURL);
        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        client.AddRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
        client.AddRequestHeader("Referer", createURL);

        client.AddHiddenParams("form");
        form = client.FindElement("form");
        client.AddFixedPostParamForm(form);

        String title = TextUtility.GenerateRandomChar(5);
        client.AddPostParam("title", title);
        client.AddPostParam("submit", "Next >>");
        client.AddPostParam("step", "1");
        client.POST(createURL);

        client.AddPostParam("title", title);
        client.AddPostParam("domain", title);
        client.AddPostParam("submit", "Next >>");
        client.AddPostParam("step", "2");
        client.AddPostParam("tld_id", "5");
        client.POST(createURL);

        client.AddPostParam("title", title);
        client.AddPostParam("domain", title);
        client.AddPostParam("category_id", "40");
        client.AddPostParam("submit", "Next >>");
        client.AddPostParam("step", "3");
        client.AddPostParam("tld_id", "5");
        client.POST(createURL);

        waitText = title;
        setWaitInfo("Blog has been created");
        navigateToPosting();
    }

    private int posting() throws Exception {
        client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        client.AddHiddenParams("form[name=edit_entry]");
        form = client.FindElement("form[name=edit_entry]");
        client.AddFixedPostParamForm(form);

        client.AddPostParam("title", TextUtility.GenerateRandomChar(5));
        client.AddPostParam("actual_date", "on");
        client.AddPostParam("mce_editor_0_fontSizeSelect", "0");
        client.AddPostParam("text", embedYoutubeCode);

        client.AddRequestHeader("Referer", postingURL);

        try {
            client.POST(postingURL);
        } catch (Exception e) {
            //this site could works very slowly and throws connection Exceptions but post anyway
            setWaitInfo("Video submitted with long timeout. Check URL:" + postingURL.replace("editEntry", "entries"));
            return 1;
        }
        setWaitInfo("submitted: " + postingURL);
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
