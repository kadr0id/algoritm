/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.captcha;

import scripting.util.concurrent.ManualResetEvent;

import java.io.InputStream;

/**
 *
 * @author GOD
 */
public class CaptchaStore {

    public String captchaText = "";
    public InputStream image;
    public boolean isStopped = false;
    public ManualResetEvent isBeingProcessed;

    public CaptchaStore(InputStream image, ManualResetEvent isBeingProcessed) {
        this.image = image;
        this.isBeingProcessed = isBeingProcessed;
        this.isStopped = false;
    }
}
