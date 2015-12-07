/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.data.entity;

/**
 *
 * @author macbookpro
 */
public class Email {

    private String body;
    private String sender;
    private int index;

    public Email(String sender, String body, int index) {
        this.body = body;
        this.sender = sender;
        this.index = index;
    }

    public String body() {
        return this.body;
    }

    public String sender() {
        return this.sender;
    }

    public int index() {
        return this.index;
    }
}
