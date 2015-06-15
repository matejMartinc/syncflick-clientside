package mmdevelopment.synchronizer; /**
 * Created by matej on 23.2.2015.
 */


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

//list view adapter
public class MyExpandableListAdapter extends BaseExpandableListAdapter {

    private final SparseArray<Group> groups;
    public LayoutInflater inflater;
    private Activity activity;
    private RelativeLayout activeVideoLayout;
    public static final String MyPREFERENCES = "MyPrefs" ;

    //used for loading thumbs
    public ImageLoader imageLoader;

    public MyExpandableListAdapter(Activity act, SparseArray<Group> groups) {
        activity = act;
        this.groups = groups;
        inflater = act.getLayoutInflater();
        imageLoader = new ImageLoader(activity.getApplicationContext());
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return groups.get(groupPosition).children.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,View convertView, ViewGroup parent) {
        final String children = (String) getChild(groupPosition, childPosition);
        TextView text = null;
        TextView user = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.listrow_details, null);
        }
        text = (TextView) convertView.findViewById(R.id.firstLine);
        user = (TextView) convertView.findViewById(R.id.secondLine);
        text.setText(children);
        if(!groups.get(groupPosition).local) {
            if(groups.get(groupPosition).users.get(childPosition)!=null)
                user.setText("Uploaded by " + groups.get(groupPosition).users.get(childPosition));
            else
                user.setText("Uploaded by unknown user");
        }
        else{
            if(groups.get(groupPosition).users.get(childPosition)!=null)
                user.setText("Made by " + groups.get(groupPosition).users.get(childPosition));
            else
                user.setText("Made by unknown user");
        }

        //download video and remove sound if clicked on anywhere but play button
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String videoUrl = groups.get(groupPosition).childrenVideoPath.get(childPosition);
                    Intent splashScreen = new Intent(activity, SplashScreenActivity.class);

                    //check if video is from server or local and put a flag in intent
                    if(groups.get(groupPosition).local)
                        splashScreen.putExtra("local", true);
                    else
                        splashScreen.putExtra("local", false);
                    splashScreen.putExtra("videoURL", videoUrl);

                    //save the name of the video
                    SharedPreferences settings;
                    SharedPreferences.Editor settingsEditor;
                    settings = activity.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                    settingsEditor = settings.edit();
                    settingsEditor.putString("videoName", groups.get(groupPosition).children.get(childPosition));
                    settingsEditor.commit();
                    activity.startActivity(splashScreen);
                    activity.finish();

                } catch (Exception e) {
                    //System.out.println(e);
                }
            }
        });

        //load thumbs
        final ImageView imageView = (ImageView) convertView.findViewById(R.id.myImage);
        imageView.setImageResource(R.drawable.stub);

        if(!groups.get(groupPosition).local) {

            imageLoader.DisplayImage(groups.get(groupPosition).childrenImagePath.get(childPosition), imageView);
        }
        else {
            new LoadLocalThumbs(activity, imageView, groups, groupPosition, childPosition).execute();
        }

        //imageview and video view visibility logic
        final VideoView videoView = (VideoView) convertView.findViewById(R.id.myVideo);
        final ImageButton play = (ImageButton) convertView.findViewById(R.id.play);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imageView.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.INVISIBLE);
                play.setVisibility(View.VISIBLE);
            }
        });
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                imageView.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.INVISIBLE);
                videoView.pause();
                videoView.seekTo(0);
                play.setVisibility(View.VISIBLE);
                return true;
            }
        });

        //preview video on thumb click
        play.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick( final View v) {

                stopVideo(activeVideoLayout);
                v.setVisibility(View.INVISIBLE);
                RelativeLayout myVideoLayout = (RelativeLayout) v.getParent();

                final ImageView imageView = (ImageView) myVideoLayout.findViewById(R.id.myImage);
                videoView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                String videoURL = groups.get(groupPosition).childrenVideoPath.get(childPosition);
                Uri vidUri = Uri.parse(videoURL);

                videoView.setVideoURI(vidUri);
                videoView.start();
                activeVideoLayout = myVideoLayout;
        }
        });
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return groups.get(groupPosition).children.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {

        super.onGroupCollapsed(groupPosition);
        stopVideo(activeVideoLayout);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.listrow_group, null);
        }
        Group group = (Group) getGroup(groupPosition);
        ((CheckedTextView) convertView).setText(group.string);
        setDrawable((CheckedTextView)convertView, group.string);
        ((CheckedTextView) convertView).setChecked(isExpanded);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private void stopVideo(RelativeLayout myVideoLayout) {
        if(myVideoLayout != null) {
            VideoView videoView =(VideoView)myVideoLayout.findViewById(R.id.myVideo);
            if(videoView.isPlaying()){
                videoView.pause();
            }
            else{
                videoView.seekTo(0);
            }

            videoView.setVisibility(View.INVISIBLE);
            ImageView imageView =(ImageView)myVideoLayout.findViewById(R.id.myImage);
            imageView.setVisibility(View.VISIBLE);
            ImageButton play = (ImageButton)myVideoLayout.findViewById(R.id.play);
            play.setVisibility(View.VISIBLE);
            activeVideoLayout = null;
        }
    }

    //set icons for categories
    private void setDrawable(CheckedTextView view, String text) {

        Drawable imageDrawable;
        if(text.equals("drama"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.drama));
        else if(text.equals("movies"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.movie_clips));
        else if(text.equals("music"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.music));
        else if(text.equals("cartoons"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.cartoon));
        else if(text.equals("funny"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.funny));
        else if(text.equals("scary"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.scary));
        else if(text.equals("WTF"))
            imageDrawable= activity.getResources().getDrawable((R.drawable.wtf));
        else
            imageDrawable= activity.getResources().getDrawable((R.drawable.camera));
        imageDrawable.setBounds(0,0, 80, 80);
        view.setCompoundDrawables(imageDrawable, null,null, null);
    }








}