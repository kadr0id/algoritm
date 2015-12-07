/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.captcha;

/**
 *
 * @author macbookpro
 */
public class ReCaptchaInfo {

    private String challengeKey;
    private String captchaURL;

    public ReCaptchaInfo(String challengeKey, String captcha) {
        this.challengeKey = challengeKey;
        this.captchaURL = captcha;
    }

    public String getCaptchaURL() {
        return this.captchaURL;
    }

    public String getChallengeKey() {
        return this.challengeKey;
    }
}
