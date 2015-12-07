/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author GOD
 */
public class AccountList {

    private int id;
    private int campaignId;
    private String name;
    private int count;

    private AccountList() {
    }

    public AccountList(int campaignId, String name) {
        this.campaignId = campaignId;
        this.name = name;
        this.count = 0;
    }

    public void setCampaignId(int campaignId) {
        this.campaignId = campaignId;
    }

    public int getCampaignId() {
        return this.campaignId;
    }

    public int getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    private transient ConcurrentHashMap<AccountItemKey, AccountItem> accounts;

    public ConcurrentHashMap<AccountItemKey, AccountItem> getAccounts() {
        synchronized (this) {
            if (accounts == null) {
                accounts = new ConcurrentHashMap<AccountItemKey, AccountItem>();
            }
        }
        return accounts;
    }

    public void saveAccount(AccountItem item) {
        ConcurrentHashMap<AccountItemKey, AccountItem> accounts = getAccounts();
        accounts.put(item.getId(), item);
        synchronized (this) {
            this.count++;
        }
    }

    public void deleteAccount(AccountItem item) {
        ConcurrentHashMap<AccountItemKey, AccountItem> accounts = getAccounts();
        accounts.remove(item.getId());
        synchronized (this) {
            this.count--;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
