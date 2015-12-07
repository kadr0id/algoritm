/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

public class AccountItemSecondKey {

    ProjectType groupName;
    int dllId;

    private AccountItemSecondKey() {
    }

    public int getID() {
        return this.dllId;
    }

    public ProjectType getGroup() {
        return this.groupName;
    }

    public AccountItemSecondKey(ProjectType groupName, int dllId) {
        this.groupName = groupName;
        this.dllId = dllId;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (groupName.hashCode());
        hash = hash * 31 + ((Integer) dllId).hashCode();
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
        final AccountItemSecondKey other = (AccountItemSecondKey) obj;
        if (this.groupName != other.groupName) {
            return false;
        }
        if (this.dllId != other.dllId) {
            return false;
        }
        return true;
    }
}
