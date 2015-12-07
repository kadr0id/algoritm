/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.captcha;

/**
 *
 * @author Doan
 */
public enum CaptchaType {

    DeathByCaptcha(0),
    Decaptcher(1),
    BypassCaptcha(2),
    BeatCaptchas(3),
    ImageTyperz(4);
    int value;

    private CaptchaType(int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }
}
