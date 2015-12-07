/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.ac.ACBlog;
import scripting.ac.ACS1;
import scripting.ac.ACS213;
import scripting.ac.ACSBlogComReg;
import scripting.ac.ACSOUP;
import scripting.data.entity.AccountItem;
import scripting.data.entity.ProjectType;
import scripting.data.entity.SiteStatus;
import scripting.data.entity.SocialNetworkProject;
import scripting.posting.BLOGIGO;
import scripting.posting.SoupIo;
import scripting.posting.THOUGHTS;
import scripting.util.http.NukeHttpClient;
import scripting.util.http.Proxy;
import sun.net.www.protocol.http.AuthCacheValue;

/**
 *
 * @author Phuc
 */
public class Scripting {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try{
            try {
                /*ACSOUP taskAccount = new ACSOUP();
                
                // init data for account task
                taskAccount.Init("www.soup.io","www.soup.io","Duong","Phuc","phucdv19861990133","1234REWq195450", "phucdv1986@yahoo.com");
                
                // run with blog.com
                taskAccount.run();*/
                
                // test soup.io posting
                /*String siteName = "https://www.soup.io";
                String site = "www.soup.io";
                String login = "phucdv19861990133";
                String password = "1234REWq195450";
                String mail = "phucdv1986@yahoo.com";
                String embedYoutubeCode = "<iframe width=\"854\" height=\"480\" src=\"https://www.youtube.com/embed/aQkp988B25k\" frameborder=\"0\" allowfullscreen></iframe>";
                SoupIo io = new SoupIo(embedYoutubeCode);
                SiteStatus siteStatus = new SiteStatus(42, ProjectType.ArticleScript, siteName);
                AccountItem myAcc = new AccountItem(1, ProjectType.ArticleScript, siteName, login, password, mail);

                io.Init(siteName, ProjectType.ArticleScript, Collections.singletonList(myAcc), siteStatus, new SocialNetworkProject(), site);
                io.run();*/
                
                // test for blogigo.com
                /*String siteName = "https://blogigo.com";
                String site = "blogigo.com";
                String login = "demo3";
                String password = "BoOTZ";
                String mail = "medemo2@gmail.com";
                String embedYoutubeCode = "<iframe width=\"854\" height=\"480\" src=\"https://www.youtube.com/embed/aQkp988B25k\" frameborder=\"0\" allowfullscreen></iframe>";
                BLOGIGO io = new BLOGIGO(embedYoutubeCode);
                SiteStatus siteStatus = new SiteStatus(42, ProjectType.ArticleScript, siteName);
                AccountItem myAcc = new AccountItem(1, ProjectType.ArticleScript, siteName, login, password, mail);

                io.Init(siteName, ProjectType.ArticleScript, Collections.singletonList(myAcc), siteStatus, new SocialNetworkProject(), site);
                io.run();*/
                
                // test for thoughts
                /*String siteName = "blog.com";
                String site = "thoughts.com";
                String login = "demo2";
                String password = "qwerty";
                String mail = "medemo2@gmail.com";
                String embedYoutubeCode = "<iframe width=\"854\" height=\"480\" src=\"https://www.youtube.com/embed/aQkp988B25k\" frameborder=\"0\" allowfullscreen></iframe>";
                THOUGHTS io = new THOUGHTS(embedYoutubeCode);
                SiteStatus siteStatus = new SiteStatus(42, ProjectType.ArticleScript, siteName);
                AccountItem myAcc = new AccountItem(1, ProjectType.ArticleScript, siteName, login, password, mail);

                io.Init(siteName, ProjectType.ArticleScript, Collections.singletonList(myAcc), siteStatus, new SocialNetworkProject(), site);
                io.run();*/
                ACSBlogComReg taskAccount = new ACSBlogComReg();
                taskAccount.Init("blog.com","blog.com","robertgift","sendor","robertgift1234","testpass11", "manly@bishailaw.com");
                taskAccount.run();
                
            } catch (Throwable ex) {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }catch(Exception ex){
        }
    }
    
    
    
}
