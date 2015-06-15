package mmdevelopment.syncserver;

import java.util.ArrayList;

/**
 * Created by matej on 23.2.2015.
 */
public class CategoryBean {
    private ArrayList<FlickBean> flickBeans;
    public  ArrayList<FlickBean> getFlickBeans() { return flickBeans; }
    public void setFlickBeans(ArrayList<FlickBean> flickBeans) { this.flickBeans= flickBeans; }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {this.name = name;}
}
