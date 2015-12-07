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
public class ACSUrlOrgReg extends ACTaskBase{

      public ACSUrlOrgReg() throws IOReactorException, NoSuchAlgorithmException, KeyManagementException, NotActivatedException {
        super();
    }
      
      private String key="6LesgMUSAAAAACkxGFkFcBeCLcXKp3oUSas32fXS";



    @Override
    public int runStep(int step) throws UnsupportedEncodingException, IOException, InterruptedException, ExecutionException, TaskStoppedException, URISyntaxException, SubmissionTimeoutException {
        int result = -1;
    
        System.out.println("Username="+username());
        System.out.println("Password="+password());
 
                client.ClearClientHeaders();
                setWaitInfo("Loading registration page...");
             
                client.Navigate("http://url.org/signup/");
                System.out.println("Page loaded!");
             
       
                setWaitInfo("Filling in registration details ...");
                
            //    client.AddHiddenParamsContains("thisform");
                 
                if (client.FindElement("input[name=query]") != null) {
                    System.out.println("Found hidden query!");
                    client.AddPostParam("query", "");
                }
                else{
                    System.out.println("Not found hiden query");
                }
            if (client.FindElement("input[name=username]") != null) {
                    System.out.println("Found username!");
                    client.AddPostParam("username", username());
                }
                else{
                    System.out.println("Not found username");
                }
                if (client.FindElement("input[name=password]") != null) {
                    System.out.println("Found password!");
                    client.AddPostParam("password", password());
                }
                else{
                    System.out.println("Not found password");
                }
             if (client.FindElement("input[name=email]") != null) {
                    System.out.println("Found email!");
                    client.AddPostParam("email", email());
                }
                else{
                    System.out.println("Not found email");
                }
            if (client.FindElement("input[name=submitted]") != null) {
                    System.out.println("Found submitted!");
                    client.AddPostParam("submitted", "Register");
                }
                else{
                    System.out.println("Not found submitted");
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
