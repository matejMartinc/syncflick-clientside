package mmdevelopment.syncserver;

import java.io.File;

/**
 * The object model for the data we are sending through endpoints
 */
public class FlickBean {
    private String category;
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {this.name = name;}

    private String user;
    public String getUser() {
        return user;
    }
    public void setUser(String user) {this.user = user;}
}