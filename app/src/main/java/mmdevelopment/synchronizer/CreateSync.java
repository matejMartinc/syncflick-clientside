package mmdevelopment.synchronizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.Formatter;
import java.util.Locale;


/**
 * Created by matej on 3.3.2015.
 */
public class CreateSync extends ActionBarActivity {

    private static final int RECORDER_SAMPLERATE = 22050;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private AsyncTask recordingThread = null;
    private boolean isRecording = false;
    private boolean back = false;
    private AudioEncoder audioEncoder = null;
    private VideoView vidView;
    private SeekBar seekBar;
    private TextView time;
    private TextView timeCurrent;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    boolean touch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_sync);
        final Activity activity = this;
        touch = false;

        //put video in vidview, generate thumbnail
        vidView = (VideoView) findViewById(R.id.myVideo);
        String videoPath = Environment.getExternalStorageDirectory().getPath() +
                File.separator + "syncflick" + File.separator + ".temp" + File.separator + "temp2.mp4";
        vidView.setVideoPath(videoPath);

        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        final ImageView imageView = (ImageView) findViewById(R.id.myImage);
        imageView.setImageBitmap(thumbnail);

        //create time slider
        time = (TextView) findViewById(R.id.time);
        timeCurrent = (TextView) findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        Button button = (Button) findViewById(R.id.start);
        seekBar = (SeekBar) findViewById(R.id.videoProgress);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!touch) {
                    touch = true;
                    imageView.setVisibility(View.INVISIBLE);
                    vidView.setVisibility(View.VISIBLE);
                    startRecording(activity);
                    vidView.start();
                }
            }
        });
        vidView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopRecording();
            }
        });

        vidView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {

                seekBar.setMax(vidView.getDuration() - 200);
                seekBar.postDelayed(onEveryDeciSecond, 100);
                time.setText(stringForTime(vidView.getDuration()));
                timeCurrent.setText(stringForTime(0));
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int originalProgress;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                originalProgress = seekBar.getProgress();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int arg1, boolean fromUser) {
                if(fromUser == true){
                    seekBar.setProgress(originalProgress);
                }
            }
        });
    }

    //update video timeline
    private Runnable onEveryDeciSecond = new Runnable() {

        @Override
        public void run() {
            if(seekBar != null) {
                seekBar.setProgress(vidView.getCurrentPosition());
            }

            if(vidView.isPlaying()) {
                timeCurrent.setText(stringForTime(vidView.getCurrentPosition()));
            }
            seekBar.postDelayed(onEveryDeciSecond, 100);



        }
    };

    //convert time to string
    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
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
            Intent about = new Intent(CreateSync.this, About.class);
            startActivity(about);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }


    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private class RecordAudio extends AsyncTask<Void, Void, Integer> {
        Activity activity;

        public RecordAudio(Activity activity) {
            this.activity=activity;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            writeAudioDataToAudioEncoder();
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {

            if(!back) {
                Intent watchSync = new Intent(CreateSync.this, WatchSync.class);

                startActivity(watchSync);
                activity.finish();

                // set up a file observer to watch this directory on sd card
                final FileObserver observer = new FileObserver(Environment.getExternalStorageDirectory().getPath() +
                        File.separator + "syncflick" + File.separator + ".temp" + File.separator + "tempsound.aac") {
                    @Override
                    public void onEvent(int event, String file) {
                        if (event == FileObserver.CLOSE_WRITE) { // check if its a "create" and not equal to .probe because thats created every time camera is launched
                            try {
                                Intent watchSync = new Intent(CreateSync.this, WatchSync.class);
                                startActivity(watchSync);
                                activity.finish();
                            } catch (Exception e) {
                                //System.out.println(e.getStackTrace());
                            }
                        }
                    }
                };
                observer.startWatching(); //START OBSERVING*/
            }
            else {
                Intent mainActivity = new Intent(CreateSync.this, MainActivity.class);

                startActivity(mainActivity);
                activity.finish();
            }

        }
    }

    private void startRecording(Activity activity) {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new RecordAudio(activity).execute();
    }



    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToAudioEncoder() {
        // Write the output audio in byte
        audioEncoder = new AudioEncoder(this);


        while (isRecording) {
            // gets the voice output from microphone to byte format
            short sData[] = new short[BufferElements2Rec];

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short wirting to file" + sData.toString());

            // // writes the data to file from buffer
            // // stores the voice buffer
            byte bData[] = short2byte(sData);
            Long audioPresentationTimeNs = System.nanoTime();
            audioEncoder.offerAudioEncoder(bData, audioPresentationTimeNs);


        }

        audioEncoder.stop();
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;

            //START OBSERVING
            //recordingThread.cancel(true);
        }
    }

    @Override
    public void onBackPressed()
    {
        if(isRecording) {
            back = true;
            stopRecording();
        }
        else {
            Intent mainActivity = new Intent(CreateSync.this, MainActivity.class);
            startActivity(mainActivity);
            this.finish();

        }
    }
}