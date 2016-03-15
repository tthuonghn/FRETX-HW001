package fretx.version1_1;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.*;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fretx.version1_1.item.SongItem;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class MainActivity extends YouTubeBaseActivity
        implements YouTubePlayer.OnInitializedListener,SearchView.OnQueryTextListener{

    MainActivity                        mActivity;

    public TextView                     m_tvConnectionState;

    public SearchView                   svNews = null;

    public ListView                     lvListNews = null;
    public ArrayList<SongItem>          mainData;
    Hashtable                           lstTimeText;
    int[]                               arrayKeys;
    Boolean[]                           arrayCallStatus;

    private static final int            RECOVERY_REQUEST = 1;

    private YouTubePlayerView           youTubeView;
    private YouTubePlayer               player;
    private MyPlayerStateChangeListener playerStateChangeListener;
    private MyPlaybackEventListener     playbackEventListener;
    public String                       videoUri;

    //add kys 0220
    private ToggleButton                tgSwitch;
    private Button                      btStartLoop;
    private Button                      btEndLoop;
    private TextView                    tvStartTime;
    private TextView                    tvEndTime;

    boolean                             bStartCheckFlag = false;        ///Flag that current time is passed start time.
    boolean                             bEndCheckFlag = false;          ///Flag that current time is passed end time.

    int                                 m_currentTime = 0;                ////Now playing time.
    int                                 durationTime = 0;               ///Video duration

    int                                 startPos = 0;                   ///start point of loop
    int                                 endPos = 0;                     ///end point of loop

    boolean                             mbLoopable = false;        ///flag of checking loop
    boolean                             mbPlaying = true;           ///Flag of now playing.


    private Handler                     mCurTimeShowHandler = new Handler();
    private Runnable                    runnable;
    boolean                             mbSendingFlag = false;

    private Button btnDownload;
    private SongListViewAdapter songListViewAdapter;


    public MainActivity() {
        runnable = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_sample);

        mActivity = this;
        Config.mActivity = this;

        initData();

        initUI();


        initYoutubeVideo();

        if (Config.bBackgroundState == true) {
            lvListNews.setVisibility(View.GONE);
        }
        showConnectionState();

    }
    @Override
    protected void onResume (){
        super.onResume();
        initData();
        initUI();
        showConnectionState();
    }

    public void showConnectionState(){
//        if (BluetoothClass.mmSocket != null){
//            if (BluetoothClass.mmSocket.isConnected()) {
//                m_tvConnectionState.setText(R.string.connect);
//                m_tvConnectionState.setBackgroundColor(Color.GREEN);
//                Config.bBlueToothActive = true;
//            }else{
//                m_tvConnectionState.setText(R.string.disconnect);
//                m_tvConnectionState.setBackgroundColor(Color.RED);
//                Config.bBlueToothActive = false;
//            }
//        }else{
//            m_tvConnectionState.setText(R.string.disconnect);
//            m_tvConnectionState.setBackgroundColor(Color.RED);
//            Config.bBlueToothActive = false;
//        }
        if (Config.bBlueToothActive == true){
            m_tvConnectionState.setText(R.string.connect);
            m_tvConnectionState.setBackgroundColor(Color.GREEN);
        }else{
            m_tvConnectionState.setText(R.string.disconnect);
            m_tvConnectionState.setBackgroundColor(Color.RED);}

    }

    public void initData(){
        btnDownload = (Button) findViewById(R.id.buttonDownloadMain);
        try {
            btnDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
                    startActivity(intent);
                }
            });
        }catch (Exception e){
            Log.e("test", e.getMessage());
        }

        File hwaccessFile = new File(this.getFilesDir().toString()+"/" + Constants.HW_BUCKET_MAPPING_FILE);
        if(hwaccessFile.isFile()){

        }

        String hw = "hw001";
        mainData = new ArrayList<SongItem>();

        File chordFileDir = new File(this.getFilesDir().toString() + "/" + hw);
        if (chordFileDir.isDirectory() && chordFileDir.listFiles() != null && chordFileDir.listFiles().length > 0){
            for(File file:chordFileDir.listFiles()) {
                String song = file.getName();
                try {
                    mainData.add(Util.setSongItem(song.substring(0, song.indexOf(".")),
                            song.substring(song.indexOf(".") + 1, song.indexOf(".txt")),
                            this.getFilesDir().toString() + "/" + hw + "/" + song));
                }catch(Exception ex){
                    mainData.add(Util.setSongItem(song,
                            song,
                            this.getFilesDir().toString() + "/" + hw + "/" + song));
                }
            }
        }else{
            new AlertDialog.Builder(this)
                    .setTitle("No song exists")
                    .setMessage("Click Download to load songs from servers")
                    .setPositiveButton(android.R.string.yes, null).create().show();
        }

        /*
        mainData.add(Util.setSongItem("The Beatles - Come Together",        "eTNitq77Utg",  R.raw.one));
        mainData.add(Util.setSongItem("The Beatles - Here Comes The Sun",   "Y6GNEEi7x4c",  R.raw.two));
        mainData.add(Util.setSongItem("Oasis - Wonderwall",                 "SLZ7uzFIMoY",  R.raw.three));
        mainData.add(Util.setSongItem("Led Zeppelin - Immigrant Song",      "TlmrQfSTmiY",  R.raw.four));
        */
    }

    public void initUI(){
        //mobile data connected?
        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean)method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {

            if(!mobileDataEnabled){
                //            Log.e("@@@@@@@@@", "aaa");
                new AlertDialog.Builder(this)
                        .setTitle("Your Network is offline")
                        .setMessage("Are you sure you want to set online?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                Intent iner = new Intent(android.provider.Settings.ACTION_SETTINGS);
                                startActivityForResult(iner, 3);

                            }
                        }).create().show();

            }
        }
        m_tvConnectionState = (TextView) findViewById(R.id.tvConnectionState);
        m_tvConnectionState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Config.bBlueToothActive == false) {
                    Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
                    startActivity(intent);
                } else {
                    try {
                        BluetoothClass.mmSocket.close();
                        Config.bBlueToothActive = false;
                        showConnectionState();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        lvListNews = (ListView)findViewById(R.id.lvSongList);
        svNews = (SearchView)findViewById(R.id.svSongs);
        svNews.setOnQueryTextListener(this);
//        svNews.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                lvListNews.setVisibility(View.VISIBLE);
//            }
//        });
        lvListNews.setAdapter(new SongListViewAdapter(this, mainData));

        btStartLoop = (Button)findViewById(R.id.btnStartLoop);  ///Button that sets startTime while playing video.
        btEndLoop = (Button)findViewById(R.id.btnEndLoop);      ///Button that sets endTime while playing video.
        ///Set startPosition of video by pressing "START LOOP" Button
        btStartLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPos = m_currentTime;
                tvStartTime.setText(String.format("%d", startPos));
            }
        });
        ///Set endPosition of video by pressing "END LOOP" Button
        btEndLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endPos = m_currentTime;
                tvEndTime.setText(String.format("%d", endPos));
            }
        });
    }

    public void initYoutubeVideo(){
        youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(Config.YOUTUBE_API_KEY, this);
        playerStateChangeListener = new MyPlayerStateChangeListener();
        playbackEventListener = new MyPlaybackEventListener();
        tgSwitch = (ToggleButton)findViewById(R.id.tgSwitch);   ///ToggleButton that sets loop.

        tvStartTime = (TextView)findViewById(R.id.tvStartTime);
        tvEndTime = (TextView)findViewById(R.id.tvEndTime);
        tgSwitch.setChecked(false);
        tvStartTime.setText("0");
        tvEndTime.setText("0");


        ///Set loopable flag.
        tgSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mbLoopable) {
                    tvStartTime.setTextColor(Color.parseColor("#000000"));
                    tvEndTime.setTextColor(Color.parseColor("#000000"));
                    mbLoopable = false;
                } else {
                    ///check current time is in duration of startPosition and endPosition.
                    String strStartPos = tvStartTime.getText().toString();
                    String strEndPos = tvEndTime.getText().toString();
                    int nTmpStartPos, nTmpEndPos;
                    if (strStartPos.length() != 0)
                        nTmpStartPos = Integer.parseInt(strStartPos);
                    else {
                        nTmpStartPos = 0;
                        tvStartTime.setText("0");
                    }
                    if (strEndPos.length() != 0)
                        nTmpEndPos = Integer.parseInt(strEndPos);
                    else {
                        nTmpEndPos = 0;
                        tvEndTime.setText("0");
                    }
                    ///if start position is bigger than end position then can not loop.
                    if (nTmpStartPos >= nTmpEndPos) {
                        tvStartTime.setTextColor(Color.parseColor("#000000"));
                        tvEndTime.setTextColor(Color.parseColor("#000000"));
                        mbLoopable = false;
                        Toast.makeText(mActivity, "Start time is bigger than End time.", Toast.LENGTH_LONG).show();
                        tgSwitch.setChecked(false);
                    } else {
                        tvStartTime.setTextColor(Color.parseColor("#FF0000"));
                        tvEndTime.setTextColor(Color.parseColor("#0000FF"));
                        getStartEndTime();
                        if ((m_currentTime < startPos) || (m_currentTime > endPos)) {
                            m_currentTime = startPos;
                            player.seekToMillis(startPos);
                        }
                        bStartCheckFlag = false;
                        bEndCheckFlag = false;
                        mbLoopable = true;
                    }
                }
            }
        });

    }

////////////////////////////////////youtubelistener///////////////////////////////////
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        this.player = youTubePlayer;
        player.setPlayerStateChangeListener(playerStateChangeListener);
        player.setPlaybackEventListener(playbackEventListener);

        if (!wasRestored) {
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_REQUEST).show();
        } else {
            String error = String.format(getString(R.string.player_error), errorReason.toString());
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    ////////////////////////////////searchlistener///////////////////////////////////
    @Override
    public boolean onQueryTextSubmit(String query) {
        lvListNews.setVisibility(View.VISIBLE);
        if (!query.equals(null)){
            ArrayList<SongItem> arrResultTemp = new ArrayList<SongItem>();
            for (int i = 0; i < mainData.size(); i ++){
                if(mainData.get(i).songName.toLowerCase().contains(query.toLowerCase())){
                    arrResultTemp.add(mainData.get(i));
                }
                lvListNews.setAdapter(new SongListViewAdapter(this, arrResultTemp));
            }
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        lvListNews.setVisibility(View.VISIBLE);
        if (newText.equals(null)){
            lvListNews.setAdapter(new SongListViewAdapter(this, mainData));
        }else{
            ArrayList<SongItem> arrResultTemp = new ArrayList<SongItem>();
            for (int i = 0; i < mainData.size(); i ++){
                if(mainData.get(i).songName.toLowerCase().contains(newText.toLowerCase())) {
                    arrResultTemp.add(mainData.get(i));

                }
                lvListNews.setAdapter(new SongListViewAdapter(this, arrResultTemp));
            }
        }
        return false;
    }
    ////////////////////////ActivityResult///////////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            getYouTubePlayerProvider().initialize(Config.YOUTUBE_API_KEY, this);
        }
    }
    //////////////backbutton//////////////////////
    @Override
    public void onBackPressed() {
        if (Config.bBackPresseOn == true){
            player.pause();
            Util.stopViaData();
            lvListNews.setVisibility(View.VISIBLE);
            Config.bBackPresseOn = false;
            Config.bBackgroundState = false;
        }
    }

    ///////////////////////add by myselft/////////////////////////////////
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return youTubeView;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void playYoutubeVideoWithUri(String str, String chordFilePath){

        initTxt(chordFilePath);

        videoUri = str;
        player.loadVideo(videoUri);
        ////This is runnable thread that sets currentTime to tvCurTime TextView and check loop
        // available through startPos and endPos
        runnable = new Runnable() {
            @Override
            public void run() {
                {
                    ///Set currentTime to current time textview.
                    if (!player.isPlaying()){
                        return;
                    }
                    m_currentTime = player.getCurrentTimeMillis();
                    ///Set the current title of current time.
                    changeText(m_currentTime);
                    if(mbLoopable){
                        if(startPos >= endPos) {
                            Toast.makeText(mActivity, "Start time is bigger than End time.", Toast.LENGTH_LONG).show();
                            mbLoopable = false;
                            tgSwitch.setChecked(false);
                        }else{
                            ///if currentTime is smaller than startPos then set bStartCheckFlag
                            // true.
                            //and set endChekFlag to false. Set current pos to startPos. and loop
                            // video.
                            if((m_currentTime < startPos) && (!bStartCheckFlag)){
                                player.seekToMillis(startPos);
                                bStartCheckFlag = true;
                                bEndCheckFlag = false;
                            }

                            ///if currentTime is bigger than startPos then set bEndCheckFlag
                            // true.
                            //and set startChekFlag to false.
                            ///Set current pos to startPos. and loop video.
                            if((m_currentTime > endPos) && (!bEndCheckFlag)){
                                bEndCheckFlag = false;
                                bStartCheckFlag = true;
                                player.seekToMillis(startPos);
                            }
                        }
                    }
                    mCurTimeShowHandler.postDelayed(this, 1);
                }
            }
        };
        mCurTimeShowHandler.post(runnable);
    }

    ///Set startPos and endPos from TextView of tvStartTime and tvEndTime
    ///Convert String data of TextView to Integer data.
    void getStartEndTime() {
        if(tvStartTime.getText().toString().length() != 0)
            startPos = Integer.parseInt(tvStartTime.getText().toString());
        else
            startPos = 0;
        if(tvEndTime.getText().toString().length() != 0)
            endPos = Integer.parseInt(tvEndTime.getText().toString());
        else
            endPos = 0;
    }

    //From the first to number of hashtable keys, Search index that its value is bigger than
    // current time. Then sets the text that was finded in hashtable keys.
    public void changeText(int currentTime) {
        for ( int nIndex = 0; nIndex < arrayKeys.length -1; nIndex++ )
        {
            if ( arrayKeys[nIndex] <= currentTime && arrayKeys[nIndex + 1] > currentTime )
            {
                if( arrayCallStatus[nIndex] )
                    return;

                arrayCallStatus[nIndex] = true;
                ConnectThread connectThread = new ConnectThread(Util.str2array((String) lstTimeText.get(arrayKeys[nIndex])));
                connectThread.run();
                Util.setDefaultValues(arrayCallStatus);
                arrayCallStatus[nIndex] = true;

            }
        }

        if ( arrayKeys[arrayKeys.length -1] <= currentTime )
        {
            if( arrayCallStatus[arrayKeys.length -1] )
                return;

            arrayCallStatus[arrayKeys.length -1] = true;
            //tvNotice.setText((String) lstTimeText.get(arrayKeys[arrayKeys.length -1]));
            ConnectThread connectThread = new ConnectThread(Util.str2array((String) lstTimeText.get(arrayKeys[arrayKeys.length - 1])));
            connectThread.run();
            Util.setDefaultValues(arrayCallStatus);
            arrayCallStatus[arrayKeys.length -1] = true;
        }


    }

    public void initTxt(String chordFilePath) {
        String str= Util.readRawTextFile(this.getBaseContext(), chordFilePath);
        String[] strArray = str.split( "\n" );
        lstTimeText = new Hashtable();
        for( int nIndex= 0; nIndex < strArray.length; nIndex++ )
        {
            ///Split the every line of source text to two parts.
            // Every line is splited by ' ',
            String[] strArrTemp = strArray[nIndex].split(" ");
            String strTime = strArrTemp[0];     ///This is time
            String strText = strArrTemp[1];     // This is text of that strTime.
            ///If ther's same time, then add two text to hashtable.
            // else add one text of the time to hashtable.
            if(lstTimeText.containsKey(Integer.parseInt(strTime)))
            // if there's same key in the
            // hashtable then add other text of same time.
            {
                String strTemp = (String)lstTimeText.get(Integer.parseInt(strTime));
                lstTimeText.put(Integer.parseInt(strTime), strTemp + ":" + strText);
            }else
                lstTimeText.put(Integer.parseInt(strTime), strText);

        }
        ///save the key array of hashtable to int array.
        arrayKeys = new int[lstTimeText.size()];
        arrayCallStatus = new Boolean[lstTimeText.size()];

        int i = 0;
        for ( Enumeration e = lstTimeText.keys(); e.hasMoreElements(); ) {
            arrayKeys[i] = (int) e.nextElement();
            arrayCallStatus[i] = false;
            i++;
        }
        Arrays.sort(arrayKeys);
    }

    ///////////////////////////////PlayEventListener///////////
    private final class MyPlaybackEventListener implements YouTubePlayer.PlaybackEventListener {

        @Override
        public void onPlaying() {
            // Called when playback starts, either due to user action or call to play().
            showMessage("Playing");
            mbPlaying = true;
            getStartEndTime();
            ////if press resume button then start loop
            mCurTimeShowHandler.post(runnable);
        }

        @Override
        public void onPaused() {
            // Called when playback is paused, either due to user action or call to pause().
            showMessage("Paused");
            mbPlaying = false;
            Util.stopViaData();
        }

        @Override
        public void onStopped() {
            // Called when playback stops for a reason other than being paused.
//            showMessage("Stopped");
            mbPlaying = false;
        }

        @Override
        public void onBuffering(boolean b) {
            // Called when buffering starts or ends.
        }

        @Override
        public void onSeekTo(int currentTime) {
            // Called when a jump in playback position occurs, either
            // due to user scrubbing or call to seekRelativeMillis() or seekToMillis()
            if (mbLoopable) {
                ///When current time is setted from seeking timeline,
                ///If current time is smaller than start position then set bStartChekFlag false.
                ///current time is smaller than start position, compare start position and
                // current time in thread.it must check in thread.
                ///Also sets bEndCheckFlag false when current time is bigger than end position.
                ///So the thread have to compare between end position and current time.
                if (currentTime < startPos)
                    bStartCheckFlag = false;
                else if (currentTime > endPos)
                    bEndCheckFlag = false;
                else
                {
                    ///When current time is in the duration of loop.
                    bStartCheckFlag = true;
                    bEndCheckFlag = false;
                }
            }
            //Set the current time of textview and change the text of current timee while
            // seeking the timeline.
            m_currentTime = currentTime;
            changeText(currentTime);
        }
    }

    ////////////////////////////////StateChangeListener//////////////
    private final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {
            // Called when the player is loading a video
            // At this point, it's not ready to accept commands affecting playback such as play() or pause()
        }

        @Override
        public void onLoaded(String s) {
            // Called when a video is done loading.
            // Playback methods such as play(), pause() or seekToMillis(int) may be called after this callback.
        }

        @Override
        public void onAdStarted() {
            // Called when playback of an advertisement starts.
        }

        @Override
        public void onVideoStarted() {
            // Called when playback of the video starts.
        }

        @Override
        public void onVideoEnded() {
            // Called when the video reaches its end.
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
            // Called when an error occurs.
        }
    }

    /////////////////////////////////BlueToothConnection/////////////////////////
    private class ConnectThread extends Thread {
        byte[] array;
        public ConnectThread(byte[] tmp) {
            array = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Util.startViaData(array);
            } catch (Exception connectException) {
                Log.i(BluetoothClass.tag, "connect failed");
                // Unable to connect; close the socket and get out
                try {
                    BluetoothClass.mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(BluetoothClass.tag, "mmSocket.close");
                }
                return;
            }
            // Do work to manage the connection (in a separate thread)
            if (BluetoothClass.mHandler == null)
                Log.v("debug", "mHandler is null @ obtain message");
            else
                Log.v("debug", "mHandler is not null @ obtain message");
            mbSendingFlag = false;
        }
    }

}
