package scripting;

import scripting.data.entity.AccountItem;
import scripting.data.entity.ProjectType;
import scripting.data.entity.SiteStatus;
import scripting.data.entity.SocialNetworkProject;
import scripting.posting.SNDiigo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @MYaskov
 */
public class TestSNDiigo {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            try {
                SNDiigo postingDiigo = new SNDiigo();

                List<AccountItem> accounts = new ArrayList<AccountItem>();
                AccountItem account = new AccountItem(1, ProjectType.SocialBlogsDiigo,"www.diigo.com","Freeforer","Freeforer123","yaskovm93@gmail.com");
                accounts.add(account);

                postingDiigo.Init("www.diigo.com", ProjectType.SocialBlogsDiigo, accounts, new SiteStatus(103,ProjectType.SocialBlogsDiigo,
                        "www.diigo.com"), new SocialNetworkProject(), "www.diigo.com");
                postingDiigo.setPostingItem("Posting title6", "posting-url5.com", "Posting description");

                postingDiigo.run();
            } catch (Throwable ex) {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }catch(Exception ex){
        }
    }
}
