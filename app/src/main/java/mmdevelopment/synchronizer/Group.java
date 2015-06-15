package mmdevelopment.synchronizer;

/**
 * Created by matej on 23.2.2015.
 */

import java.util.ArrayList;
import java.util.List;


//create groups from data from server - used in list view
public class Group {

    public boolean local;
    public String string;
    public final List<String> children = new ArrayList();
    public final List<String> childrenImagePath = new ArrayList();
    public final List<String> childrenVideoPath = new ArrayList();
    public final List<String> users = new ArrayList();
    public final List<Long> childrenImageID = new ArrayList();

    public Group(String string) {
        this.string = string;
    }

}