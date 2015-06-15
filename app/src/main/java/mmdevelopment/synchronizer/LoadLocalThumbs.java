package mmdevelopment.synchronizer;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.widget.ImageView;


/**
 * Created by matej on 24.3.2015.
 */

//load thumb from images on sd card
public class LoadLocalThumbs extends AsyncTask<Void, Void, Integer> {
    private Activity activity;
    private SparseArray<Group> groups;
    private ImageView imageView;
    private int groupPosition;
    private int childPosition;
    private Bitmap thumb;

    public LoadLocalThumbs(Activity activity, ImageView imageView, SparseArray<Group> groups, int groupPosition, int childPosition) {
        this.groups = groups;
        this.imageView = imageView;
        this.groupPosition = groupPosition;
        this.childPosition = childPosition;
        this.activity = activity;

    }

    @Override
    protected Integer doInBackground(Void... params) {
        ContentResolver crThumb = activity.getContentResolver();
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize = 1;
        thumb = MediaStore.Video.Thumbnails.getThumbnail(crThumb, groups.get(groupPosition).childrenImageID.get(childPosition), MediaStore.Video.Thumbnails.MICRO_KIND, options);
        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        imageView.setImageBitmap(thumb);
    }
}
