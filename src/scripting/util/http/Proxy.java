/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

/**
 *
 * @author Doan
 */
public class Proxy {

    private String username;
    private String password;
    private String address;
    private int port;

    public Proxy(String address, int port, String username, String password) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Proxy(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }
}
