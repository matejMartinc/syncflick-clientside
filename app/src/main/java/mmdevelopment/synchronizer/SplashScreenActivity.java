package mmdevelopment.synchronizer;

import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class SplashScreenActivity extends ActionBarActivity {

    String videoUrl;
    Boolean local;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        //instantiate load spinner
        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            videoUrl = extras.getString("videoURL");
            local = extras.getBoolean("local");
        }
        new DownloadVideo().execute();
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
            Intent about = new Intent(SplashScreenActivity.this, About.class);
            startActivity(about);
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private class DownloadVideo extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if(!local)
                    downloadVideo(videoUrl);
            } catch (Exception e) {
                //System.out.println(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {

            //delete temp file if exists
            try {
                File temp2 = new File(Environment.getExternalStorageDirectory().getPath() +
                        File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp2.mp4");
                if(temp2.exists())
                    temp2.delete();
                cloneMediaUsingMuxer(Environment.getExternalStorageDirectory().getPath() +
                        File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp2.mp4");
            } catch(Exception e) {
                //System.out.println(e);
            }
            Intent createSync = new Intent(SplashScreenActivity.this, CreateSync.class);
            startActivity(createSync);
            finish();
        }
    }

    private void downloadVideo(String videoURL) throws Exception {
        final int TIMEOUT_CONNECTION = 5000;//5sec
        final int TIMEOUT_SOCKET = 30000;//30sec


        URL url = new URL(videoURL);
        long startTime = System.currentTimeMillis();


        //Open a connection to that URL.
        URLConnection ucon = url.openConnection();

        //this timeout affects how long it takes for the app to realize there's a connection problem
        ucon.setReadTimeout(TIMEOUT_CONNECTION);
        ucon.setConnectTimeout(TIMEOUT_SOCKET);


        //Define InputStreams to read from the URLConnection.
        // uses 3KB download buffer
        InputStream is = ucon.getInputStream();
        BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
        File temp = new File(Environment.getExternalStorageDirectory().getPath() +
                File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp.mp4");
        if(temp.exists())
            temp.delete();
        FileOutputStream outStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath() +
                File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp.mp4"));
        byte[] buff = new byte[5 * 1024];

        //Read bytes (and store them) until there is nothing more to read(-1)
        int len;
        while ((len = inStream.read(buff)) != -1) {
            outStream.write(buff, 0, len);
        }

        //clean up
        outStream.flush();
        outStream.close();
        inStream.close();
    }

    private void cloneMediaUsingMuxer(String dstMediaPath) throws IOException {

        int MAX_SAMPLE_SIZE = 256 * 1024;

        // Set up MediaExtractor to read from the source.
        MediaExtractor extractor = new MediaExtractor();
        if(!local)
            extractor.setDataSource(Environment.getExternalStorageDirectory().getPath() +
                    File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp.mp4");
        else
            extractor.setDataSource(videoUrl);
        int trackCount = extractor.getTrackCount();

        // Set up MediaMuxer for the destination.
        MediaMuxer muxer;
        muxer = new MediaMuxer(dstMediaPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        // Set up the tracks.
        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            extractor.selectTrack(i);
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video/")) {
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
            }
        }

        // Copy the samples from MediaExtractor to MediaMuxer.
        boolean sawEOS = false;
        int bufferSize = MAX_SAMPLE_SIZE;
        int offset = 100;
        long lastTimeStamp = 0;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();


        muxer.start();
        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
            if (bufferInfo.size < 0) {
                sawEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                if(bufferInfo.presentationTimeUs < lastTimeStamp) {
                    bufferInfo.presentationTimeUs = lastTimeStamp + 1;
                }
                lastTimeStamp = bufferInfo.presentationTimeUs;
                bufferInfo.flags = extractor.getSampleFlags();
                int trackIndex = extractor.getSampleTrackIndex();
                if(indexMap.get(trackIndex) != null) {
                    muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                            bufferInfo);
                }
                extractor.advance();
            }
        }
        muxer.stop();
        muxer.release();

        return;
    }


}
