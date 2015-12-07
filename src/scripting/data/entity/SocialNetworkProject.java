/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import scripting.util.TextUtility;

import java.util.HashSet;
import java.util.List;

/**
 *
 * @author macbookpro
 */
public class SocialNetworkProject extends BaseProject {
    protected String fName;
    protected String lName;
    protected String username;
    protected String password;
    protected String email;
    protected String keywords;
    protected String tags;
    protected String subdomain;
    protected String anchor;
    private String title;
    private boolean createNewDomain = true;
    protected String body;
    protected String domain;
    protected int urlListID = -1;
    protected transient List<String> rotatedDomains;
    private transient HashSet<String> listSite = new HashSet<String>();

    public SocialNetworkProject() {
        super();
        this.type = ProjectType.SocialNetwork;
    }

    public SocialNetworkProject(String name, int parentId) {
        super(name, parentId);
        this.type = ProjectType.SocialNetwork;
    }

    public void setProfileId(int profileId) {
        if (!this.profileIds.contains(profileId)) {
            this.profileIds.add(profileId);
        }
    }

    public void removeProfileId(int profileId) {
        for (Integer id : this.profileIds) {
            if (id == profileId) {
                this.profileIds.remove(id);
                return;
            }
        }
    }

    public List<Integer> getProfileIds() {
        return this.profileIds;
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

    public String getKeywords() {
        return this.keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getTags() {
        return this.tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAnchor() {
        return this.anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public boolean isCreateNewDomain() {
        return this.createNewDomain;
    }

    public void setCreateNewDomain(boolean createNewDomain) {
        this.createNewDomain = createNewDomain;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return this.body;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return this.domain;
    }

    public int getURLListID() {
        return this.urlListID;
    }

    public void setURLListID(int listID) {
        this.urlListID = listID;
    }

    public void rotateDomains() {
        this.rotatedDomains = TextUtility.GenerateRandomVariations(domain);
    }

    public List<String> getRotatedDomains() {
        return this.rotatedDomains;
    }

    public void addListSite(String site) {
        if (listSite.contains(site)) {
            this.listSite.add(site);
        }
    }

    public boolean isListSiteContain(String site) {
        if (this.listSite.contains(site)) {
            return true;
        }
        return false;

    }

    public HashSet<String> getListSite() {
        return this.listSite;
    }

    public String checkInputs() {
        String message = "";
        boolean ok = true;
        if (this.getURLListID() == -1) {
            message = message + "Please choose an URL List\r\n";
            ok = false;
        }

        if (this.getProfileIds().isEmpty()) {
            message = message + "Please choose an Account Profile\r\n";
            ok = false;
        }
        return message;
    }
}
