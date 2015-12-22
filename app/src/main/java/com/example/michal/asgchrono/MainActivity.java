package com.example.michal.asgchrono;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.ProgressBar;

public class MainActivity
        extends AppCompatActivity
        implements HistoryFragment.OnFragmentInteractionListener
{
    public final String TAG = "AsgChrono";

    /// recorder constants
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /// audio recorder members
    private Thread recordingThread = null;
    private volatile boolean isRecording = false;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private AsgCounter mAsgCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager)findViewById(R.id.container);
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mAsgCounter = new AsgCounter();
        startRecording();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopRecording();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    int BufferElements2Rec = 1024;  // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2;  // 2 bytes in 16bit format

    private void startRecording() {
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                readAudioData();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording()  {
        isRecording = false;
        Log.i(TAG, "Waiting for recorder thread...");
        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Recorder thread interrupted");
        }

        Log.i(TAG, "Activity destroyed");
    }

    volatile float maxVolume;

    private void readAudioData() {
        AudioRecord recorder;
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        if (recorder == null) {
            Log.e(TAG, "Recorder failed to start");
            return;
        }

        recorder.startRecording();

        short sData[] = new short[BufferElements2Rec];
        Log.i(TAG, "Recording started...");

        while (isRecording) {
            if (recorder.read(sData, 0, BufferElements2Rec) == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.e(TAG, "AudioRecord.read failed");
                break;
            }

            maxVolume = 0.0f;
            for (int i = 0; i < BufferElements2Rec; ++i) {
                float sample = (float)sData[i];
                if (sample > maxVolume)
                    maxVolume = sample;
            }
            maxVolume /= 32768.0f;

            // update GUI
            mViewPager.post(new Runnable() {
                @Override
                public void run() {
                    ProgressBar bar = (ProgressBar)findViewById(R.id.progressBar_monitor);
                    if (bar != null)
                        bar.setProgress((int)((float)bar.getMax() * maxVolume));
                }
            });
        }

        recorder.release();
        Log.i(TAG, "Exiting recorder thread");
    }

    public void onFragmentInteraction(int param){
        // TODO: fragments interaction
    }

    /**
     * @class SectionsPagerAdapter
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        String[] mTabNames;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mTabNames = getResources().getStringArray(R.array.tab_names);
        }

        // getItem is called to instantiate the fragment for the given page.
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return MeasureFragment.newInstance();
                case 1:
                    return HistoryFragment.newInstance();
                case 2:
                    return HistoryFragment.newInstance();
                /*
                    return new PreferenceFragment() {
                        @Override
                        public void onCreate(Bundle savedInstanceState) {
                            super.onCreate(savedInstanceState);
                            addPreferencesFromResource(R.xml.setup_preference);
                        }
                    };
                    */

            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabNames[position];
        }
    }
}
