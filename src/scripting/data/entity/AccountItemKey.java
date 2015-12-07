/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

/**
 *
 * @author GOD
 */
public class AccountItemKey {

    int accountListId;
    ProjectType groupName;
    String siteName;
    String email;

    private AccountItemKey() {
    }

    public String getSiteName() {
        return this.siteName;
    }
    
    public String getEmail(){
        return this.email;
    }

    public ProjectType getGroup() {
        return this.groupName;
    }

    public AccountItemKey(int accountListId, ProjectType groupName, String siteName, String email) {
        this.accountListId = accountListId;
        this.groupName = groupName;
        this.siteName = siteName;
        this.email = email;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + ((Integer) accountListId).hashCode();
        hash = hash * 31 + (groupName.hashCode());
        hash = hash * 31 + ((String) siteName).hashCode();
        hash = hash * 31 + ((String) email).hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AccountItemKey other = (AccountItemKey) obj;
        if (this.accountListId != other.accountListId) {
            return false;
        }
        if (this.groupName != other.groupName) {
            return false;
        }
        if (this.siteName != other.siteName) {
            return false;
        }
        if (this.email != other.email){
            return false;
        }
        return true;
    }
}
