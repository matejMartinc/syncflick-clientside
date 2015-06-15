/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package mmdevelopment.syncserver;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

import java.util.ArrayList;
import java.util.List;


/**
 * An endpoint class we are exposing
 */
@Api(name = "taskApi", version = "v1", namespace = @ApiNamespace(ownerDomain = "syncserver.mmdevelopment", ownerName = "syncserver.mmdevelopment", packagePath = ""))
public class MyEndpoint {

    @ApiMethod(name = "storeTask")
    public void storeTask(FlickBean flickBean) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        TransactionOptions options = TransactionOptions.Builder.withXG(true);
        Transaction txn = datastoreService.beginTransaction(options);
        try {
            String category = flickBean.getCategory();
            Key categoryKey = KeyFactory.createKey(category, "syncflick");
            Query query = new Query(categoryKey);
            List<Entity> results = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());
            if(results.isEmpty()){
                Key taskBeanParentKey = KeyFactory.createKey("syncflickParent", "syncflick");
                Entity taskEntity = new Entity("TaskBean", taskBeanParentKey);
                taskEntity.setProperty("category", category);
                datastoreService.put(taskEntity);
            }
            Entity taskEntity = new Entity("TaskBean", categoryKey);
            taskEntity.setProperty("name", flickBean.getName());
            taskEntity.setProperty("category", category);
            taskEntity.setProperty("user", flickBean.getUser());
            datastoreService.put(taskEntity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @ApiMethod(name = "getTasks")
    public List<CategoryBean> getTasks() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Key taskBeanParentKey = KeyFactory.createKey("syncflickParent", "syncflick");
        Query query = new Query(taskBeanParentKey);
        List<Entity> results = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        ArrayList<CategoryBean> categoryBeans = new ArrayList();
        for (Entity result : results) {
            CategoryBean categoryBean = new CategoryBean();
            String category = (String) result.getProperty("category");
            categoryBean.setName(category);
            Key categoryKey = KeyFactory.createKey(category, "syncflick");
            Query flickQuery = new Query(categoryKey);
            List<Entity> flickResults = datastoreService.prepare(flickQuery).asList(FetchOptions.Builder.withDefaults());
            ArrayList<FlickBean> flickBeans = new ArrayList();
            for(Entity flickResult : flickResults) {
                FlickBean flickBean = new FlickBean();
                flickBean.setName((String) flickResult.getProperty("name"));
                flickBean.setCategory((String) flickResult.getProperty("category"));
                flickBean.setUser((String) flickResult.getProperty("user"));
                flickBeans.add(flickBean);
            }
            categoryBean.setFlickBeans(flickBeans);
            categoryBeans.add(categoryBean);
        }

        return categoryBeans;
    }

    @ApiMethod(name = "clearTasks")
    public void clearTasks() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastoreService.beginTransaction();
        try {
            Key taskBeanParentKey = KeyFactory.createKey("TaskBeanParent", "todo.txt");
            Query query = new Query(taskBeanParentKey);
            List<Entity> results = datastoreService.prepare(query)
                    .asList(FetchOptions.Builder.withDefaults());
            for (Entity result : results) {
                datastoreService.delete(result.getKey());
            }
            txn.commit();
        } finally {
            if (txn.isActive()) { txn.rollback(); }
        }
    }

}
