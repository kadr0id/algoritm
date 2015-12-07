/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util;

/**
 *
 * @author Doan
 */
public class EzineInfo {

    private String body;
    private String title;
    private String tags;

    public String getBody() {
        return this.body;
    }

    public String getTitle() {
        return this.title;
    }

    public String getTags() {
        return this.tags;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public EzineInfo(String title, String body, String tags) {
        this.title = title;
        this.body = body;
        this.tags = tags;
    }
}
