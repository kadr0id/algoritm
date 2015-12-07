package scripting.posting;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.base.SNTaskBase;
import scripting.util.http.RequestHelper;

/**
 *   Diigo.com
 * @ MYaskov
 */
public class SNDiigo extends SNTaskBase{

    private Element form;
    private String submitUrl;
    private PostingItem postingItem;
    private String mainUserPage;

    public SNDiigo() throws Throwable {
        super();
    }

    public void setPostingItem(String title, String url, String description) {
        postingItem = new PostingItem(title, url, description);
    }

    public void setPostingItem(String title, String url, String privateModeOpt, String unreadOpt, String description, String tags, String list, String groups) {
        postingItem = new PostingItem(title, url, privateModeOpt, unreadOpt, description, tags, list, groups);
    }

    @Override
    public int runStep(int step) throws Throwable {
        int result = -1;

        switch(step) {
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Loggin in...");

                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.Navigate(String.format("https://%s/", site)); // format string
                String signInButton = client.FindElement("li#SignInButton a[href]").attr("href");
                client.Navigate(signInButton);

                nextStep = nextStep + 1;

                break;
            case 2:
                form = client.FindElement("form[action*=/sign-in]");

                client.AddFixedPostParamForm(form);
                client.AddPostParam("username", username());
                client.AddPostParam("password", password());

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                submitUrl = client.getCurrentURL();

                if (form != null && form.hasAttr("action")) {
                    submitUrl = RequestHelper.MakeAbsoluteURL(client.getCurrentURL(), form.attr("action"));
                }

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                client.POST(submitUrl);

                nextStep = nextStep + 1;

                break;
            case 3:
                if(client.responseContains("Username does't exist.|WRONG PASSWORD.")) {
                    setWaitInfo("Login failed.");
                    result = SkipSiteOrRetry();
                } else {
                    setWaitInfo("Login succeded.");

                    mainUserPage = String.format("https://%s/user/%s", site, username().toLowerCase());
                    client.Navigate(mainUserPage);

                    nextStep = nextStep + 1;
                }
                break;
            case 4: //add bookmark
                if (postingItem != null) {
                    setWaitInfo("Posting pending...");

                    Elements items = client.FindElements("div#ditemItemsWraper [id*=ditemItem]"); //find existing items

                    postingItem.dindex = String.valueOf(items.size());

                    client.AddPostParam("title", postingItem.title);
                    client.AddPostParam("url", postingItem.url);
                    client.AddPostParam("private", postingItem.privateModeOpt);
                    client.AddPostParam("unread", postingItem.unreadOpt);
                    client.AddPostParam("description", postingItem.description);
                    client.AddPostParam("tags", postingItem.tags);
                    client.AddPostParam("list", postingItem.list);
                    client.AddPostParam("groups", postingItem.groups);
                    client.AddPostParam("dindex", postingItem.dindex);

                    client.AddRequestHeader("Referer", client.getCurrentURL());
                    client.AddRequestHeader("Accept", "text/html, */*");
                    client.AddRequestHeader("X-Requested-With", "XMLHttpRequest");

                    client.POST(String.format("https://%s/item/save/bookmark", site));

                    nextStep = nextStep + 1;
                } else {
                    setWaitInfo("Fail: Nothing to post.");
                    result = 0;
                }
                break;
            case 5: //check posting
                client.Navigate(mainUserPage);
                Element newItem = client.FindElement("div#ditemItemsWraper").getElementById("ditemItem_" + postingItem.dindex);

                if (newItem != null) {
                    setWaitInfo("Posting succeded.");
                    setWaitInfo("Done.");
                    result = 1;
                } else {
                    newItem = client.FindElement("div#ditemItemsWraper a:contains("+postingItem.url+")");
                    if (newItem != null) {
                        setWaitInfo("Posting with url: "+postingItem.url+" was changed.");
                        setWaitInfo("Done.");
                        result = 1;
                    } else {
                        setWaitInfo("Posting failed.");
                        result = SkipSiteOrRetry();
                    }
                }

                break;
        }
        return result;
    }

    private class PostingItem {
        String title;
        String url;
        String privateModeOpt; //true, false
        String unreadOpt;   //true, false
        String description;
        String tags; //set up by space separation
        String list;
        String groups;
        String dindex;

        PostingItem(String title, String url, String description) {
            if (title.equals("")) {
                this.title = "untitled";
            } else {
                this.title = title;
            }
            if (url.equals("")) {
                this.url = "example.com";
            } else {
                this.url = url;
            }
            this.privateModeOpt = "false";
            this.unreadOpt = "false";
            this.description = description;
            this.tags = "";
            this.list = "";
            this.groups = "";
        }

        PostingItem(String title, String url, String privateModeOpt, String unreadOpt, String description, String tags, String list, String groups) {
            if (title.equals("")) {
                this.title = "untitled";
            } else {
                this.title = title;
            }
            if (url.equals("")) {
                this.url = "example.com";
            } else {
                this.url = url;
            }
            if (Boolean.valueOf(privateModeOpt)) {
                this.privateModeOpt = privateModeOpt.toLowerCase();
            } else {
                this.privateModeOpt = "false";
            }
            if (Boolean.valueOf(unreadOpt)) {
                this.unreadOpt = unreadOpt.toLowerCase();
            } else {
                this.unreadOpt = "false";
            }
            this.description = description;
            this.tags = tags;
            this.list = list;
            this.groups = groups;
        }
    }
}
