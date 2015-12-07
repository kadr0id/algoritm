/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import scripting.util.email.ServerType;

/**
 *
 * @author GOD
 */
public class Verification {

    public String name;
    public String popServer;
    public String userName;
    public String passWord;
    public int popPort = 995;
    public boolean SSL = true;
    public ServerType serverType;
    public String spamFolderName;
    public boolean checkSpamFolder;
    public boolean useAPOP;
}
