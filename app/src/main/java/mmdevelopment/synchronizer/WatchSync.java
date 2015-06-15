package mmdevelopment.synchronizer;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class WatchSync extends ActionBarActivity {

    String videoName;
    String videoPath;

    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences settings;

    CallbackManager callbackManager;
    ShareDialog shareDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_sync);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        videoPath = Environment.getExternalStorageDirectory().getPath() +
                File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp3.mp4";

        File temp3 = new File(videoPath);
        if(temp3.exists())
            temp3.delete();

        cloneMediaUsingMuxer(videoPath);


        Button redo =(Button)findViewById(R.id.redo);
        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createSync = new Intent(WatchSync.this, CreateSync.class);
                startActivity(createSync);
                finish();
            }
        });
        Button save =(Button)findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                settings = WatchSync.this.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                videoName = settings.getString("videoName","video");
                String name = videoName + "_" + timeStamp+".mp4";
                moveFile(videoPath, Environment.getExternalStorageDirectory().getPath() +
                        File.separator + "syncflick" + File.separator + "video" + File.separator + name, name );
                new SingleMediaScanner(WatchSync.this, Environment.getExternalStorageDirectory().getPath() +
                        File.separator + "syncflick" + File.separator + "video" + File.separator + name);

            }
        });
        Button share =(Button)findViewById(R.id.share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File videoFile = new File(videoPath);
                if (videoFile.length()/1024/1024 > 12 ) {
                    Toast.makeText(getApplicationContext(), "Video needs to be smaller than 12mb to get published on facebook",
                            Toast.LENGTH_LONG).show();
                }
                else {

                    if (ShareDialog.canShow(ShareVideoContent.class)) {

                        Uri videoFileUri = Uri.fromFile(videoFile);
                        ShareVideo video = new ShareVideo.Builder()
                                .setLocalUrl(videoFileUri)
                                .build();
                        ShareVideoContent content = new ShareVideoContent.Builder()
                                .setVideo(video)
                                .build();
                        shareDialog.show(content);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please download latest facebook application",
                                Toast.LENGTH_LONG).show();
                    }
                }

            }
        });


        final VideoView vidView = (VideoView)findViewById(R.id.myVideo);
        final ImageButton play = (ImageButton)findViewById(R.id.play);
        vidView.setVideoPath(videoPath);
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        final ImageView imageView = (ImageView) findViewById(R.id.myImage);
        imageView.setImageBitmap(thumbnail);
        //final MediaController vidControl = new MediaController(this);
        //vidControl.setAnchorView(vidView);
        //vidView.setMediaController(vidControl);
        vidView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                play.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
                vidView.setVisibility(View.INVISIBLE);
            }
        });

        vidView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent even) {
                play.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
                vidView.setVisibility(View.INVISIBLE);
                vidView.pause();
                vidView.seekTo(0);
                return true;
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( final View v) {
                v.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                vidView.setVisibility(View.VISIBLE);
                vidView.start();
            }
        });
        vidView.start();
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
            Intent about = new Intent(WatchSync.this, About.class);
            startActivity(about);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void cloneMediaUsingMuxer(String dstMediaPath)  {
        boolean VERBOSE = true;
        int MAX_SAMPLE_SIZE = 256 * 1024;

        // Set up MediaExtractor to read from the source.


        MediaExtractor soundExtractor = new MediaExtractor();
        try {
            soundExtractor.setDataSource(Environment.getExternalStorageDirectory().getPath() +
                    File.separator + "syncflick" + File.separator + ".temp" + File.separator + "tempsound.aac");
        } catch(IOException e) {
            //System.out.println(e.getMessage());
        }
        int soundTrackCount = soundExtractor.getTrackCount();

        MediaExtractor videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(Environment.getExternalStorageDirectory().getPath() +
                    File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp2.mp4");
        } catch(IOException e) {
            //System.out.println(e.getMessage());
        }
        int videoTrackCount = videoExtractor.getTrackCount();

        // Set up MediaMuxer for the destination.
        MediaMuxer muxer;
        try {
            muxer = new MediaMuxer(dstMediaPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch(IOException e) {
            //System.out.println(e.getMessage());
            return;
        }

        // Set up the tracks.
        HashMap<Integer, Integer> soundIndexMap = new HashMap<Integer, Integer>(soundTrackCount);
        HashMap<Integer, Integer> videoIndexMap = new HashMap<Integer, Integer>(videoTrackCount);
        for (int i = 0; i < videoTrackCount; i++) {
            videoExtractor.selectTrack(i);
            MediaFormat format = videoExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video/")) {
                int dstIndex = muxer.addTrack(format);
                videoIndexMap.put(i, dstIndex);
            }
        }
        for (int i = 0; i < soundTrackCount; i++) {
            soundExtractor.selectTrack(i);
            MediaFormat format = soundExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                int dstIndex = muxer.addTrack(format);
                soundIndexMap.put(i, dstIndex);
            }
        }

        // Copy the samples from MediaExtractor to MediaMuxer.
        boolean sawVideoEOS = false;
        int videoBufferSize = MAX_SAMPLE_SIZE;
        int videoFrameCount = 0;
        int videoOffset = 100;
        ByteBuffer videoDstBuf = ByteBuffer.allocate(videoBufferSize);
        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();

        boolean sawAudioEOS = false;
        int audioBufferSize = MAX_SAMPLE_SIZE;
        int audioFrameCount = 0;
        int audioOffset = 100;
        ByteBuffer audioDstBuf = ByteBuffer.allocate(audioBufferSize);
        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


        muxer.start();
        while (!sawVideoEOS || !sawAudioEOS) {
            videoBufferInfo.offset = videoOffset;
            videoBufferInfo.size = videoExtractor.readSampleData(videoDstBuf, videoOffset);
            audioBufferInfo.offset = audioOffset;
            audioBufferInfo.size = soundExtractor.readSampleData(audioDstBuf, audioOffset);

            if(!sawVideoEOS && !sawAudioEOS) {
                audioBufferInfo.flags = soundExtractor.getSampleFlags();
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                audioBufferInfo.presentationTimeUs = soundExtractor.getSampleTime();


                if(videoBufferInfo.presentationTimeUs < audioBufferInfo.presentationTimeUs) {
                    int trackIndex = videoExtractor.getSampleTrackIndex();
                    if(videoIndexMap.get(trackIndex) != null) {
                        muxer.writeSampleData(videoIndexMap.get(trackIndex), videoDstBuf,
                                videoBufferInfo);
                    }
                    videoExtractor.advance();
                    videoFrameCount++;
                }
                else {
                    int trackIndex = soundExtractor.getSampleTrackIndex();
                    if(soundIndexMap.get(trackIndex) != null) {
                        muxer.writeSampleData(soundIndexMap.get(trackIndex), audioDstBuf,
                                audioBufferInfo);
                    }
                    soundExtractor.advance();
                    audioFrameCount++;
                }
            }

            else if(!sawAudioEOS) {
                audioBufferInfo.flags = soundExtractor.getSampleFlags();
                audioBufferInfo.presentationTimeUs = soundExtractor.getSampleTime();




                int trackIndex = soundExtractor.getSampleTrackIndex();
                if(soundIndexMap.get(trackIndex) != null) {
                    muxer.writeSampleData(soundIndexMap.get(trackIndex), audioDstBuf, audioBufferInfo);
                }
                soundExtractor.advance();
                audioFrameCount++;
            }
            else if (!sawVideoEOS) {
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();



                int trackIndex = videoExtractor.getSampleTrackIndex();
                if(videoIndexMap.get(trackIndex) != null) {
                    muxer.writeSampleData(videoIndexMap.get(trackIndex), videoDstBuf,videoBufferInfo);
                }
                videoExtractor.advance();
                videoFrameCount++;
            }
            if (videoBufferInfo.size < 0) {

                sawVideoEOS = true;
                videoBufferInfo.size = 0;
            }
            if (audioBufferInfo.size < 0) {

                sawAudioEOS = true;
                audioBufferInfo.size = 0;
            }
        }

        muxer.stop();
        muxer.release();
        return;
    }

    @Override
    public void onBackPressed()
    {
        Intent mainActivity = new Intent(WatchSync.this, MainActivity.class);
        startActivity(mainActivity);
        this.finish();
    }

    //save file in gallery
    private void moveFile(String inputPath, String outputPath, String name) {

        InputStream in = null;
        OutputStream out = null;
        try {

            in = new FileInputStream(inputPath);
            out = new FileOutputStream(outputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // save message
            Toast.makeText(getApplicationContext(), "Video saved as " + name,
                    Toast.LENGTH_LONG).show();
            }

        catch (FileNotFoundException fnfe1) {
            //Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            //Log.e("tag", e.getMessage());
        }

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
