/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

/**
 *
 * @author macbookpro
 */
public class SiteStatusKey {

    int projectID;
    ProjectType groupName;
    String siteName;

    private SiteStatusKey() {
    }

    public SiteStatusKey(int projectID, ProjectType groupName, String siteName) {
        this.projectID = projectID;
        this.groupName = groupName;
        this.siteName = siteName;
    }

    public ProjectType getGroup() {
        return this.groupName;
    }

    public String getSiteName() {
        return this.siteName;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + ((Integer) projectID).hashCode();
        hash = hash * 31 + (groupName.hashCode());
        hash = hash * 31 + ((String) siteName).hashCode();
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
        final SiteStatusKey other = (SiteStatusKey) obj;
        if (this.projectID != other.projectID) {
            return false;
        }
        if (this.groupName != other.groupName) {
            return false;
        }
        if (this.siteName != other.siteName) {
            return false;
        }
        return true;
    }
}
