/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import com.google.common.base.Strings;

/**
 *
 * @author GOD
 */
public class AccountCreationProject extends BaseProject {

    protected String fName = "";
    protected String lName = "";
    protected String username = "";
    protected String password = "";
    protected String email = "";
    protected boolean autoCreateEmail = true;
    protected String verifyEmail = "";
    protected String verifyPassword = "";
    protected String mailServer = "";
    protected int mailPort = 995;
    protected boolean useSSL = true;
    protected boolean verifyAfterComplete = true;

    public AccountCreationProject() {
        super();
        this.type = ProjectType.AccountCreation;
    }

    public AccountCreationProject(String name, int parentId) {
        super(name, parentId);
        this.type = ProjectType.AccountCreation;
    }

    public void setProfileId(int profileId) {
        this.profileIds.clear();
        this.profileIds.add(profileId);
//        if (this.profileIds.size() == 1) {
//            this.profileIds.set(0, profileId);
//        } else this.profileIds.add(profileId);
    }

    public void removeProfileId(int profileId) {
        for (Integer id : this.profileIds) {
            if (id == profileId) {
                this.profileIds.remove(id);
                return;
            }
        }
    }

    public int getProfileId() {
        if (this.profileIds.size() >= 1) {
            return this.profileIds.get(0);
        } else {
            return -1;
        }
    }

    public void setFName(String fName) {
        this.fName = fName;
    }

    public String getFName() {
        return this.fName;
    }

    public void setLName(String lName) {
        this.lName = lName;
    }

    public String getLName() {
        return this.lName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public int getURLListID() {
        return -1;
    }

    public void setURLListID(int listID) {
    }

    public String checkInputs() {
        boolean result = true;
        String message = "";
        if (Strings.isNullOrEmpty(this.getFName())) {
            message = message + "First name can not be empty \r\n";
            result = false;
        }
        if (Strings.isNullOrEmpty(this.getLName())) {
            message = message + "Last name can not be empty \r\n";
            result = false;
        }
        if (Strings.isNullOrEmpty(this.getUsername())) {
            message = message + "Username can not be empty \r\n";
            result = false;
        }
        if (Strings.isNullOrEmpty(this.getPassword())) {
            message = message + "Password can not be empty \r\n";
            result = false;
        }
        return message;
    }

    public boolean isAutoCreateEmail() {
        return this.autoCreateEmail;
    }

    public void setAutoCreateEmail(boolean autoCreateEmail) {
        this.autoCreateEmail = autoCreateEmail;
    }

    public String getVerifyEmail() {
        return this.verifyEmail;
    }

    public void setVerifyEmail(String email) {
        this.verifyEmail = email;
    }

    public String getVerifyPassword() {
        return this.verifyPassword;
    }

    public void setVerifyPassword(String password) {
        this.verifyPassword = password;
    }

    public String getMailServer() {
        return this.mailServer;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public int getMailPort() {
        return this.mailPort;
    }

    public void setMailPort(int port) {
        this.mailPort = port;
    }

    public boolean isUseSSL() {
        return this.useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isVerifyAfterComplete() {
        return this.verifyAfterComplete;
    }

    public void setVerifyAfterComplete(boolean verifyAfterComplete) {
        this.verifyAfterComplete = verifyAfterComplete;
    }
}
