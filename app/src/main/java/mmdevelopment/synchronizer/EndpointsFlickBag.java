package mmdevelopment.synchronizer;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mmdevelopment.syncserver.taskApi.TaskApi;
import mmdevelopment.syncserver.taskApi.model.CategoryBean;
import mmdevelopment.syncserver.taskApi.model.FlickBean;

/**
 * Created by matej on 18.2.2015.
 */

public class EndpointsFlickBag {
    final TaskApi taskApiService;

    // Constructor
    public EndpointsFlickBag() {

        TaskApi.Builder builder = new TaskApi.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setApplicationName("red-delight-860")
                .setRootUrl("https://red-delight-860.appspot.com/_ah/api")
                .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                                                       @Override
                                                       public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                                                               throws IOException {
                                                           abstractGoogleClientRequest.setDisableGZipContent(true);
                                                       }
                                                   }
                );
        taskApiService = builder.build();
    }

    //get data from server
    public synchronized List pullFromRemote() {

        List<CategoryBean> remoteTasks;
        try {
            // Remote Call
            remoteTasks = taskApiService.getTasks().execute().getItems();
        } catch (IOException e) {
            //Log.e(EndpointsFlickBag.class.getSimpleName(), "Error when loading tasks", e);
            remoteTasks = new ArrayList<>();
        }

        return remoteTasks;
    }
}