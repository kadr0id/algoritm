/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.ac;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.apache.http.nio.reactor.IOReactorException;
import scripting.base.ACTaskBase;
import scripting.exception.NotActivatedException;
import scripting.exception.SubmissionTimeoutException;
import scripting.exception.TaskStoppedException;

/**
 *
 * @author Acid Serg
 */
public class ACSPostoliaReg extends ACTaskBase{
     public ACSPostoliaReg() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }
      
     
private String key="6LfwKQQAAAAAAPFCNozXDIaf8GobTb7LCKQw54EA";



    @Override
    public int runStep(int step) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException {
        int result = -1;
    
        System.out.println("Username="+username());
        System.out.println("Password="+password());
 
  
                client.ClearClientHeaders();

                setWaitInfo("Loading registration page...");
             
                client.Navigate("http://postolia.com/ybwz3W/");
                System.out.println("Page loaded!");
             
               
       
                setWaitInfo("Filling in registration details ...");
                
                client.AddHiddenParamsContains("thisform");
                 
                if (client.FindElement("input[name=reg_username]") != null) {
                    System.out.println("Found Username!");
                    client.AddPostParam("reg_username", username());
                }
                else{
                    System.out.println("Not found Username");
                }

                if (client.FindElement("input[name=reg-checkbutton1]") != null) {
                    System.out.println("Found reg-checkbutton1!");
                    client.AddPostParam("reg-checkbutton1","Verify");
                }
                else{
                    System.out.println("Not found reg-checkbutton1!");
                }
                
                if (client.FindElement("input[name=reg-checkbutton2]") != null) {
                    System.out.println("Found reg-checkbutton2!");
                    client.AddPostParam("reg-checkbutton2","Verify");
                }
                else{
                    System.out.println("Not found reg-checkbutton1!");
                }
                
                if (client.FindElement("input[name=reg_email]") != null) {
                    System.out.println("Found email!");
                    client.AddPostParam("reg_email", email());
                }
                else{
                    System.out.println("Not found email");
                }
                 if (client.FindElement("input[name=reg_password]") != null) {
                    System.out.println("Found password1!");
                    client.AddPostParam("reg_password", password());
                }
                else{
                    System.out.println("Not found password1");
                }
                   if (client.FindElement("input[name=reg_password2]") != null) {
                    System.out.println("Found password2!");
                    client.AddPostParam("reg_password2", password());
                }
                else{
                    System.out.println("Not found password2");
                }
                   if (client.FindElement("input[name=recaptcha_response_field]") != null) {
                    System.out.println("Found Hiden recaptcha_response_field");
                    client.AddPostParam("recaptcha_response_field", "manual_challenge");
                }
                else{
                    System.out.println("Not found Hiden recaptcha_response_field");
                }
                
            if (client.FindElement("input[name=submit]") != null) {
                    System.out.println("Found submit");
                    client.AddPostParam("submit", "Create user");
                }
                else{
                    System.out.println("Not found submit");
                }
            if (client.FindElement("input[name=regfrom]") != null) {
                    System.out.println("Found hiden regform");
                    client.AddPostParam("regfrom", "full");
                }
                else{
                    System.out.println("Not found Hiden regform");
                }
            
            System.out.println("Solving google captcha");
               SolveGoogleCaptcha(key);
               setWaitInfo("Submitting registration...");

                client.AddRequestHeader("Referer", client.getCurrentURL());
                client.AddRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                client.POST(client.getCurrentURL());
                waitText = "The Image Phrase entered is incorrect|Your registration is complete|Your registration is now complete|This email address is already|*message";
          
   
                setUsername(email());
                setWaitInfo("Account Created");
                result = 1;
          
                System.out.println("REGISTRATION DONE!");
    return result;

    }
}
