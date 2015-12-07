/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.captcha;

import scripting.util.captcha.CaptchaStore;

import java.util.LinkedList;

/**
 *
 * @author Doan
 */
public class CaptchaManager {

    private LinkedList<CaptchaStore> captchas;
    private CaptchaStore currentCaptcha;

    public CaptchaManager() {
        captchas = new LinkedList<CaptchaStore>();
    }

    public CaptchaStore getCaptcha() {
        return this.captchas.poll();
    }

    public synchronized void addCaptcha(CaptchaStore captcha) {
        this.captchas.add(captcha);
    }
}
