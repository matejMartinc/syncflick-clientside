package mmdevelopment.synchronizer;

import android.app.Activity;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.widget.ExpandableListView;
import java.util.List;

import mmdevelopment.syncserver.taskApi.model.CategoryBean;
import mmdevelopment.syncserver.taskApi.model.FlickBean;

/**
 * Created by matej on 18.2.2015.
 */

public class PullAsyncTask extends AsyncTask<Void, Void, Integer> {

    static final int SUCCESS = 0;
    static final int ERROR = 2;
    static String TAG = "pulltask";
    private Activity activity;
    private List<CategoryBean> categoryBeans;
    SparseArray<Group> groups = new SparseArray();
    MyExpandableListAdapter adapter;


    private EndpointsFlickBag endpoints;

    public PullAsyncTask(Activity activity) {
        this.endpoints = new EndpointsFlickBag();
        this.activity = activity;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            categoryBeans = endpoints.pullFromRemote();
            createData();

        } catch (Exception e) {
            return ERROR;
        }

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {

        //get video paths and names from external storage
        listAllLocalVideos();

        //create adapter
        ExpandableListView listView = (ExpandableListView) this.activity. findViewById(R.id.listView);
        adapter = new MyExpandableListAdapter(this.activity, groups);
        listView.setAdapter(adapter);
    }

    //get all the paths and names of thumbs from the server
    public void createData() {
        for (int j = 0; j < categoryBeans.size(); j++) {
            String name = categoryBeans.get(j).getName();
            List<FlickBean> flickBeans = categoryBeans.get(j).getFlickBeans();
            Group group = new Group(name);
            group.local = false;
            for (FlickBean flickBean : flickBeans) {
                String flickName = flickBean.getName();
                group.children.add(flickName.replace("_", " "));
                String imageUrl = "http://storage.googleapis.com/syncflick.appspot.com/"+flickName+".jpg";
                String videoUrl = "http://storage.googleapis.com/syncflick.appspot.com/"+flickName+".mp4";
                group.childrenImagePath.add(imageUrl);
                group.childrenVideoPath.add(videoUrl);
                group.users.add(flickBean.getUser());
            }
            groups.append(j, group);
        }
    }

    //get all paths and names of videos from internal storage and put them in groups
    public void listAllLocalVideos() {
        String name = "my videos";
        Group group = new Group(name);
        group.local = true;
        String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.ARTIST };
        Cursor cursor = new CursorLoader(activity, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                null, // Return all rows
                null, null).loadInBackground();

        while(cursor.moveToNext()) {
            group.children.add(cursor.getString(2));
            group.childrenImageID.add(cursor.getLong(0));
            group.childrenVideoPath.add(cursor.getString(1));
            group.users.add(cursor.getString(3));
        }
        groups.append(groups.size(), group);
    }
}
