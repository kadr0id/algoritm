/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

/**
 *
 * @author GOD
 */
public class AccountItem {
//    @PrimaryKey(sequence="accountid")
//    private long id;
//    

    private int accountListId;
    private AccountItemSecondKey accountSecondKey;
    private AccountItemKey id;
    private ProjectType groupName;
    private String username;
    private String password;
    private String email;
    private String page;

    private AccountItem() {
    }

    public AccountItem(int accountListId, ProjectType groupName, String siteName, String username, String password, String email) {
        this.id = new AccountItemKey(accountListId, groupName, siteName, email);
        this.accountSecondKey = new AccountItemSecondKey(groupName, 0);
        this.username = username;
        this.password = password;
        this.email = email;
        this.accountListId = accountListId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUserName(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AccountItemKey getId() {
        return this.id;
    }

    public int getAccountListId() {
        return this.accountListId;
    }

    public AccountItemSecondKey getSecondKey() {
        return this.accountSecondKey;
    }

    public void setSecondKey() {
        this.accountSecondKey = new AccountItemSecondKey(this.id.getGroup(), 0);
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPage() {
        return this.page;
    }
}
