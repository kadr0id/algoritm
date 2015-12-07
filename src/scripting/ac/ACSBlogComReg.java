/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scripting.base.ACTaskBase;
import scripting.captcha.ReCaptchaInfo;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;
import scripting.util.TextUtility;
import scripting.util.http.NukeHttpClient;

/**
 *
 * @author Acid Serg
 */
public class ACSBlogComReg extends ACTaskBase{
  //  http://blog.com/wp-signup.php
    public ACSBlogComReg() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }



    public String randomSignUpFor()
    {
        String blog = "blog";
        String user = "user";
        int randVal;
   
        Random rand = new Random();
        randVal=rand.nextInt(2);
        if(randVal==0)
        {
            return blog;
        }
        else
        {
            return user;
        }
    }
  
      
     
   private String key="TB.Z2YlpJGPvV8ugArjCqyjzHv3FBj4a";



    @Override
    public int runStep(int step) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException {
        int result = -1;
        
        System.out.println("E-mail="+email());
        System.out.println("Password="+password());
    
        switch(step){
            case 1:
                client.ClearClientHeaders();

                setWaitInfo("Loading registration page...");

                client.Navigate("http://"+ siteName + "/wp-signup.php");
                System.out.println("Page loaded!");
                nextStep++;
             break;   
            case 2:
                setWaitInfo("Filling in registration details ...");

                client.AddHiddenParamsContains("setupform");

                if (client.FindElement("input[name=stage]") != null) {
                    System.out.println("Found Hiden element STAGE!");
                    client.AddPostParam("stage", "validate-user-signup");
                }
                else{
                    System.out.println("Not found Hiden element STAGE");
                }
                if (client.FindElement("input[name=signup_form_id]") != null) {
                    System.out.println("Found Hiden element SIGN UP FOR ID");
                    client.AddPostParam("signup_form_id", "1279478317");
                }
                else{
                    System.out.println("Not found Hiden element SIGN UP FOR ID");
                }
                if (client.FindElement("input[name=_signup_form]") != null) {
                    System.out.println("Found Hiden element _SIGN UP FOR ID");
                    client.AddPostParam("_signup_form", "47dd257c55");
                }
                else{
                    System.out.println("Not found Hiden element _SIGN UP FOR ID");
                 }

                if (client.FindElement("input[name=user_email]") != null) {
                    System.out.println("Found email!");
                    client.AddPostParam("user_email", email());
                }
                else{
                    System.out.println("Not found Email");
                }
                if (client.FindElement("input[name=password_1]") != null) {
                    System.out.println("Found password1!");
                    client.AddPostParam("password_1", password());
                }
                else{
                    System.out.println("Not found password_1");
                }
                if (client.FindElement("input[name=password_2]") != null) {
                    System.out.println("Found password2!");
                    client.AddPostParam("password_2", password());
                }
                else{
                    System.out.println("Not found password_2");
                }
                if (client.FindElement("input[name=signup_for]")!= null) {
                    System.out.println("Found sign up for radio buttons");
                    client.AddPostParam("signup_for", randomSignUpFor());
                }
                else{
                    System.out.println("Not found sign up for radio buttons");
                }

                if(client.FindElement("input[name=submit_btn]")!=null)
                {
                    client.AddPostParam("submit_btn", "Next &rarr");
                }
                else
                {
                    System.out.println("Cant found signup button");
                }
                nextStep++;
                break;
            case 3:
                SolveMediaCaptcha("TB.Z2YlpJGPvV8ugArjCqyjzHv3FBj4a");
                nextStep++;
                break;
            case 4:
                setWaitInfo("Submitting registration...");

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST(client.getCurrentURL());
                waitText = "The Image Phrase entered is incorrect|Your registration is complete|Your registration is now complete|This email address is already|*message";

                setUsername(email());
                setWaitInfo("Account Created");
                result = 1;
                System.out.println("REGISTRATION DONE!");
                
        }
        return result;
    }     
        

        
      

}


