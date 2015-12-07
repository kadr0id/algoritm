/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

/**
 *
 * @author Doan
 */
public enum ProjectStatus {

    SCHEDULED_RUNNING("Scheduled Running"),
    RUNNING("Running"),
    NONE(""),
    COMPLETED(""),
    COMPLETED_FOR_THE_DAY("");
    private String strValue = "";

    ProjectStatus(String value) {
        strValue = value;
    }

    public String toString() {
        return strValue;
    }
}
