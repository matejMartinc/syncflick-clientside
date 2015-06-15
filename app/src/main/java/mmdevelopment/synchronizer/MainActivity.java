package mmdevelopment.synchronizer;


import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends ActionBarActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new PullAsyncTask(this).execute();
        File appStorageDir = new File(Environment.getExternalStorageDirectory(), "syncflick");
        if (!appStorageDir.exists()){
            if (!appStorageDir.mkdirs()){
                Toast.makeText(getApplicationContext(), "Unable to write on disk. Application is shutting down.", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
            else {
                File tempDir = new File(appStorageDir.getPath() + File.separator + ".temp");
                File videoDir = new File(appStorageDir.getPath() + File.separator + "video");
                if (!tempDir.mkdirs() || !videoDir.mkdir()){
                    Toast.makeText(getApplicationContext(), "Unable to write on disk. Application is shutting down.", Toast.LENGTH_LONG).show();
                    System.exit(0);
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.upload) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://syncflick.appspot.com/"));
            startActivity(browserIntent);
        }
        if (id == R.id.about) {
            Intent about = new Intent(MainActivity.this, About.class);
            startActivity(about);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        System.exit(0);
    }
}
