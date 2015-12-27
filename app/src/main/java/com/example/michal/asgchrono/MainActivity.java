package com.example.michal.asgchrono;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements HistoryFragment.OnFragmentInteractionListener, MeasureFragment.MeasureFragmentInteractionListener
{
    public static int PARAM_RESET = 0;
    public static int PARAM_SAVE = 1;
    public static int PARAM_SHOW_ADVANCED_VIEW = 2;
    public static int PARAM_HIDE_ADVANCED_VIEW = 3;

    public final String TAG = "AsgChrono";

    public final double METERS_TO_FEETS = 3.2808;
    public final double SECS_TO_MINS = 60.0;

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
    private List<HistoryEntry> mHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager)findViewById(R.id.container);
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

        // TODO: load from file
        mHistoryList = new ArrayList<>();

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mAsgCounter = new AsgCounter();
        mAsgCounter.config.sampleRate = (float)RECORDER_SAMPLERATE;
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
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
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
        float fData[] = new float[BufferElements2Rec];

        Log.i(TAG, "Recording started...");

        while (isRecording) {
            if (recorder.read(sData, 0, BufferElements2Rec) == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.e(TAG, "AudioRecord.read failed");
                break;
            }

            maxVolume = 0.0f;
            for (int i = 0; i < BufferElements2Rec; ++i) {
                float sample = (float)sData[i] / 32768.0f;
                fData[i] = sample;
                if (sample > maxVolume)
                    maxVolume = sample;
            }

            boolean needUpdate = false;
            synchronized (mAsgCounter) {
                needUpdate = mAsgCounter.ProcessBuffer(fData, BufferElements2Rec);
            }

            if (needUpdate) {
                // update GUI
                mViewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        updateStats();
                    }
                });
            }

            // update GUI
            mViewPager.post(new Runnable() {
                @Override
                public void run() {
                    ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar_monitor);
                    if (bar != null)
                        bar.setProgress((int) ((float) bar.getMax() * maxVolume));
                }
            });
        }

        recorder.release();
        Log.i(TAG, "Exiting recorder thread");
    }

    private void resetCounter() {
        synchronized (mAsgCounter) {
            mAsgCounter.Reset();
        }

        // update GUI
        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                updateStats();
            }
        });
    }

    private void updateStats() {

        // TODO: this function should only update GUI
        AsgStats stats = mAsgCounter.stats;
        synchronized (mAsgCounter) {
            mAsgCounter.stats.Calc(mAsgCounter.config);
        }

        TextView textView;

        // velocity stats

        textView = (TextView) findViewById(R.id.textView_velocity);
        if (textView != null)
            if (stats.velocityAvg > 0.0)
                textView.setText(String.format("%.1f", stats.velocityAvg * METERS_TO_FEETS));
            else
                textView.setText(R.string.not_analyzed_text);

        textView = (TextView) findViewById(R.id.textView_min_velocity);
        if (textView != null)
            if (stats.velocityAvg > 0.0)
                textView.setText(String.format("%.1f", stats.velocityMin * METERS_TO_FEETS));
            else
                textView.setText(R.string.not_analyzed_text);

        textView = (TextView) findViewById(R.id.textView_max_velocity);
        if (textView != null)
            if (stats.velocityAvg > 0.0)
                textView.setText(String.format("%.1f", stats.velocityMax * METERS_TO_FEETS));
            else
                textView.setText(R.string.not_analyzed_text);

        textView = (TextView) findViewById(R.id.textView_std_velocity);
        if (textView != null)
            if (stats.velocityAvg > 0.0)
                textView.setText(String.format("%.2f", stats.velocityStdDev * METERS_TO_FEETS));
            else
                textView.setText(R.string.not_analyzed_text);

        // fire rate stats

        textView = (TextView) findViewById(R.id.textView_firerate);
        if (textView != null)
            if (stats.fireRateAvg > 0.0)
                textView.setText(String.format("%.1f", stats.fireRateAvg * SECS_TO_MINS));
            else
                textView.setText(R.string.not_analyzed_text);

        textView = (TextView) findViewById(R.id.textView_min_firerate);
        if (textView != null)
            if (stats.fireRateAvg > 0.0)
                textView.setText(String.format("%.1f", stats.fireRateMin * SECS_TO_MINS));
            else
                textView.setText(R.string.not_analyzed_text);

        textView = (TextView) findViewById(R.id.textView_max_firerate);
        if (textView != null)
            if (stats.fireRateAvg > 0.0)
                textView.setText(String.format("%.1f", stats.fireRateMax * SECS_TO_MINS));
            else
                textView.setText(R.string.not_analyzed_text);

        // misc stats

        // TODO

        ListView list = (ListView) findViewById(R.id.listView_sampleHistory);
        ArrayList<String> stringsList = new ArrayList<>();
        for (int i = 0; i < stats.history.size(); ++i)
            stringsList.add(String.format("#%-3d %10.1f %10.1f", i, stats.history.get(i).velocity,
                                          stats.history.get(i).deltaTime));

        list.setAdapter(new ArrayAdapter<String>(this, R.layout.sample_row, stringsList));
    }

    public void onFragmentInteraction(int param){
        // TODO
    }


    public void onResetButtonClicked() {
        resetCounter();
    }

    public void onSaveButtonClicked() {

        final Context context = this;
        final AsgStats stats = mAsgCounter.stats;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Save measurement");
        alert.setMessage("Enter name:");

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mHistoryList.add(new HistoryEntry(input.getText().toString(), stats.velocityAvg, stats.fireRateAvg));

                ListView listView = (ListView) findViewById(R.id.listView_history);
                HistoryViewAdapter adapter = new HistoryViewAdapter(context, R.layout.history_row, mHistoryList);
                listView.setAdapter(adapter);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
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
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabNames[position];
        }
    }
}
