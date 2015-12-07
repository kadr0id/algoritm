/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.captcha;

import com.DeathByCaptcha.CancellableHttpClient;
import com.DeathByCaptcha.Captcha;
import scripting.util.concurrent.ManualResetEvent;
import scripting.exception.TaskStoppedException;

import javax.swing.*;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

//import scraper.Main;

/**
 *
 * @author GOD
 */
public class CaptchaWork {

    class CaptchaRunnable implements Runnable {

        CancellableHttpClient client;
        
        public String deabthByCaptchaUserName;
        public String deabthByCaptchaPassword;
        
        public CaptchaRunnable(String username, String password)
        {
            this.deabthByCaptchaUserName = username;
            this.deabthByCaptchaPassword = password;
        }
        
        // default constructor
        public CaptchaRunnable()
        {
            
        }

        @Override
        public void run() {
            //client = (Client) new SocketClient(MainApp.nukeSettings.getDeathByCaptchaUsername(), MainApp.nukeSettings.getDeathByCaptchaPassword());
            client = new CancellableHttpClient(deabthByCaptchaUserName, deabthByCaptchaPassword);
            client.setExecutorService(service);
            client.setStopEvent(CaptchaWork.this.store.isBeingProcessed);
            //client.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888));
            try {
                Captcha captcha = client.decode(CaptchaWork.this.store.image, 120);
                if (captcha.isSolved() && captcha.isCorrect()) {
                    System.out.printf("CAPTCHA(id:text) %s: %s", captcha.id, captcha.text);
                    CaptchaWork.this.store.captchaText = captcha.text;

                    // Report the CAPTCHA if solved incorrectly.
                    // Make sure the CAPTCHA was in fact incorrectly solved!
                    //CaptchaWork.this.store.isBeingProcessed.setSource("completed");
                }

            } catch (Exception ex) {
                System.out.println(ex);
            } finally {
                //CaptchaWork.this.store.isBeingProcessed.setSource("completed");
                //CaptchaWork.this.store.isBeingProcessed.reset();
                client.close();
            }
        }

        public void stop() {
            client.close();
        }
    }
    private CaptchaStore store;
    private ExecutorService service;

    public CaptchaWork(InputStream image, ManualResetEvent stopEvent) {
        this.store = new CaptchaStore(image, stopEvent);
    }

    public void setExecutorService(ExecutorService service) {
        this.service = service;
    }

    public void process() throws InterruptedException, TaskStoppedException {
        CaptchaRunnable k = null;
        /*if (!MainApp.nukeSettings.isUseCaptchaService()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    //MainApp.captchaDialog.setModal(false);
                    //BufferedImage image = ImageIO.read(store.image);
                    //Flipper.dialog.setImage(image);
                    MainApp.captchaDialog.addCaptcha(store);
                    //MainApp.captchaDialog.setVisible(true);
                }
            });
            try {
                this.store.isBeingProcessed.waitOne();
                if (this.store.isBeingProcessed.getSource().equals("cancelled")) {
//                    if (k != null) {
//                        k.stop();
//                    }
                    throw new TaskStoppedException("Task stopped");
                }
            } finally {
                this.store.isBeingProcessed.reset();
            }
        } else {*/
            k = new CaptchaRunnable("sdseoblog", "Wx310zs."); // TODO add input captcha username and password here
            k.run();
    }

    public String getCaptcha() {
        return store.captchaText;
    }

    public void stop() {
        synchronized (this) {
            this.store.isStopped = true;
            this.store.isBeingProcessed.set();
        }
    }
}
