package scripting.ac;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.nodes.Element;

import scripting.base.ACTaskBase;
import scripting.exception.NotActivatedException;

public class ACBlog extends ACTaskBase {

	private Element form;

	public ACBlog() throws IOReactorException, NoSuchAlgorithmException,
			KeyManagementException, NotActivatedException {
		super();
	}

	@Override
	public int runStep(int step) throws Throwable {
		int result = -1;

		switch (step) {
		case 1:

			setUsername(email().split("@")[0]);
			client.ClearClientHeaders();
			client.Navigate("http://" + site);
			setWaitInfo("Logging in...");

			nextStep = nextStep + 1;
			break;

		case 2:

			form = client.FindElement("form[id=loginform]");
			client.AddFixedPostParamForm(form);

			client.AddPostParam("log", email());
			client.AddPostParam("pwd", password());
			client.AddRequestHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			client.AddRequestHeader("Referer", client.getCurrentURL());

			client.POST("http://blog.com/wp-login.php");

			setWaitInfo("Logged in");
			nextStep = nextStep + 1;
			break;

		case 3:

			client.Navigate("http://" + username()
					+ ".blog.com/wp-admin/post-new.php");
			client.AddHiddenParams("form[id=post]");

			form = client.FindElement("form[id=post]");
			client.AddFixedPostParamForm(form);

			client.AddPostParam(
					"content",
					"<a href='https://www.youtube.com/watch?v=cjH6VC87xKo' >Fall-Winter 2015/16 Haute Couture CHANEL Show</a>");
			client.AddPostParam("visibility", "Public");
			client.AddPostParam("post_format", "Standard");
			client.AddRequestHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			client.AddRequestHeader("Referer", client.getCurrentURL());

			client.POST("post.php");

			result = 1;
			break;
		}
		return result;
	}

}
