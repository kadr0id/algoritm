/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.exception;

/**
 *
 * @author GOD
 */
public class TaskStoppedException extends Exception {

    public TaskStoppedException(String task_stopped) {
        super(task_stopped);
    }
}
