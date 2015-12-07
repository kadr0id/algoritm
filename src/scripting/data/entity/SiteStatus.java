/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 *
 * @author macbookpro
 */
public class SiteStatus {

    private SiteStatusKey id;
    private int projectID;
    private Status status;
    private boolean checked = false;
    private Date lastPosted;
    private String domains = "";
    private String categoryValue = "";
    private transient HashMap<Integer, String> domainsMap;
    private String lastWaitInfo = "";
    private transient boolean isRunning = false;

    private SiteStatus() {
    }

    public SiteStatus(int projectID, ProjectType group, String siteName) {
        this.id = new SiteStatusKey(projectID, group, siteName);
        this.status = Status.NONE;
        this.checked = false;
        this.projectID = projectID;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized Status getStatus() {
        return this.status;
    }

    public SiteStatusKey getID() {
        return this.id;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public synchronized HashMap<Integer, String> getDomainsMap() {
        if (domains == null || domains.equals("")) {
            domainsMap = new HashMap<Integer, String>();
            return domainsMap;
        }
        if (domainsMap == null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                TypeReference<HashMap<Integer, String>> typeRef = new TypeReference<HashMap<Integer, String>>() {
                };
                domainsMap = mapper.readValue(domains, typeRef);
                return domainsMap;
            } catch (IOException ex) {
                Logger.getLogger(SiteStatus.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            return domainsMap;
        }
    }

    public void setDomain(int profileID, String domain) {
        if (domainsMap == null) {
            getDomainsMap();
        }
        domainsMap.put(profileID, domain);
        try {
            domains = (new ObjectMapper()).writeValueAsString(domainsMap);
        } catch (IOException ex) {
            Logger.getLogger(SiteStatus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getDomain(int profileID) {
        if (domainsMap == null) {
            getDomainsMap();
        }
        return domainsMap.get(profileID);
    }

    public String getCategoryValue() {
        return this.categoryValue;
    }

    public void setCategoryValue(String value) {
        this.categoryValue = value;
    }

    public void setWaitInfo(String waitInfo) {
        this.lastWaitInfo = waitInfo;
    }

    public String getWaitInfo() {
        return this.lastWaitInfo;
    }

    public synchronized void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public synchronized boolean isRunning() {
        return this.isRunning;
    }
}
