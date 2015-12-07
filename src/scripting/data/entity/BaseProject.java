/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author GOD
 */
public abstract class BaseProject {

    protected int id;
    protected int campaignId;
    protected String name;
    protected ProjectType type;
    protected Date dateCreated;
    protected Date dateModified;
    protected List<Integer> profileIds;
    protected MetaData metaData;
    protected boolean running = false;
    protected ProjectStatus status = ProjectStatus.NONE;
    protected boolean startedByScheduler = false;
    protected transient boolean aborted;
    protected transient boolean isInQueue = false;
    protected boolean isDeleted = false;

    protected BaseProject() {
    }

    public BaseProject(String name, int campaignId) {
        this.name = name;
        this.campaignId = campaignId;
        this.profileIds = new ArrayList<Integer>();
        this.dateCreated = new Date();
        this.dateModified = new Date();
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return id;
    }

    public List<Integer> getProfileIds() {
        return profileIds;
    }

    public void setProfileIds(List<Integer> ids) {
        this.profileIds = ids;
    }

    public void setProfileId(int id) {
        this.profileIds.add(id);
    }

    public void removeProfileId(int id) {
        this.profileIds.remove(id);
    }

    public MetaData getMetaData() {
        return this.metaData;
    }

    public void setMetaData(MetaData data) {
        this.metaData = data;
    }

    public boolean canBeScheduled() {
        return true;
    }

    public boolean showSitesDue() {
        return true;
    }

    public ProjectType getType() {
        return this.type;
    }

    public int getCampaignID() {
        return this.campaignId;
    }

    public Date getLastModified() {
        return this.dateModified;
    }

    public ProjectType getGroup() {
        return this.type;
    }

    public boolean isRunning() {
        //return this.running;
        return this.status == ProjectStatus.RUNNING || this.status == ProjectStatus.SCHEDULED_RUNNING;
    }

    public ProjectStatus getStatus() {
        return this.status;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isInQueue() {
        return this.isInQueue;
    }

    public void setInQueue(boolean inQueue) {
        this.isInQueue = inQueue;
    }

    public void setStartedByScheduler(boolean startedByScheduler) {
        this.startedByScheduler = startedByScheduler;
    }

    public boolean isStartedByScheduler() {
        return this.startedByScheduler;
    }

    public abstract int getURLListID();

    public abstract void setURLListID(int listID);

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public abstract String checkInputs();

    private transient ReentrantLock lock = new ReentrantLock();

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
