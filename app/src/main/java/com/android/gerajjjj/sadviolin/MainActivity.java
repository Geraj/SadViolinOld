package com.android.gerajjjj.sadviolin;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

/**
 *
 */
public class MainActivity extends Activity {
    private static final String TAG = "Sad-Violin" ;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private MySensorListener mMySensorListener = new MySensorListener();
    private MediaPlayer mMediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView view = (ImageView)findViewById(R.id.imageView);
        view.setImageDrawable(getResources().getDrawable(R.drawable.violintrans2));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();


        //createSoundPool();
        mMediaPlayer = MediaPlayer.create(this, R.raw.sadviolin);

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mSensorManager.registerListener(mMySensorListener, mAccelerometer,
                        SensorManager.SENSOR_DELAY_UI);
            }
        });


    }

//    SoundPool
//    private SoundPool mSoundPool;
//    protected void createSoundPool() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            createNewSoundPool();
//        } else {
//            createOldSoundPool();
//        }
//    }
//
//    private void createNewSoundPool() {
//        AudioAttributes attributes = new AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                .build();
//
//        mSoundPool = new SoundPool.Builder()
//                .setAudioAttributes(attributes)
//                .setMaxStreams(NR_OF_STREAMS)
//                .build();
//    }
//
//    @SuppressWarnings("deprecation")
//    protected void createOldSoundPool() {
//        mSoundPool = new SoundPool(NR_OF_STREAMS, AudioManager.STREAM_MUSIC, 0);
//    }

    @Override
    protected void onPause() {
        mMediaPlayer.release();
        mSensorManager.unregisterListener(mMySensorListener);
        super.onPause();
    }

    private class MySensorListener implements SensorEventListener{

        private final float mAlpha = 0.8f;
        // Arrays for storing filtered values
        private float[] mGravity = new float[3];
        private float[] mAccel = new float[3];
        private long mLastUpdate, mPreviousActionCount;

        private static final int MAX_VALID_ACTION_COUNT = 10;
        private static final int START_ACTION_COUNT = -5;
        private static final int START_SONG_ACTION_COUNT = 8;
        private static final int STOP_SONG_ACTION_COUNT = 6;
        private static final float CONSIDERED_ACTION = 1.5f;

        MySensorListener(){
            mPreviousActionCount = START_ACTION_COUNT;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long actualTime = System.currentTimeMillis();
                if (actualTime - mLastUpdate > 100) {
                    mLastUpdate = actualTime;

                    float rawX = event.values[0];
                    float rawY = event.values[1];
                    float rawZ = event.values[2];
                    // Apply low-pass filter
                    mGravity[0] = lowPass(rawX, mGravity[0]);
                    mGravity[1] = lowPass(rawY, mGravity[1]);
                    mGravity[2] = lowPass(rawZ, mGravity[2]);
                    mAccel[0] = highPass(rawX, mGravity[0]);
                    mAccel[1] = highPass(rawY, mGravity[1]);
                    mAccel[2] = highPass(rawZ, mGravity[2]);

                    boolean isAction = Math.abs(mAccel[0]) + Math.abs(mAccel[1]) + Math.abs(mAccel[2]) > CONSIDERED_ACTION;
                    //Log.i(TAG, "Action value: " +  Math.abs(mAccel[0])+Math.abs(mAccel[1])+ Math.abs(mAccel[2]) + " " +  mAccel[0] + " " + mAccel[1] + " " + mAccel[2] );
                    //take into acount the previous actions
                    if (isAction) {
                        if (mPreviousActionCount <= MAX_VALID_ACTION_COUNT) {
                            mPreviousActionCount++;
                        }
                    } else {
                        if (mPreviousActionCount > 0) {
                            mPreviousActionCount--;
                        }
                    }
                    if (isAction) {
                        if (!mMediaPlayer.isPlaying() && mPreviousActionCount > START_SONG_ACTION_COUNT) {
                            Log.i(TAG, "Start " + mLastUpdate);
                            //mSoundPool.play(mSoundID, mStreamVolume, mStreamVolume, 10, -1, 1);
                            mMediaPlayer.start();
                        }
                    }
                    if (!isAction && mMediaPlayer.isPlaying() && mPreviousActionCount < STOP_SONG_ACTION_COUNT) {
                        Log.i(TAG, "Pause " + mLastUpdate);
                        mMediaPlayer.pause();
                        //mSoundPool.pause(mSoundID);
                        //isPlaying = false;
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // DO NOTHING
        }

        // Deemphasize constant forces
        private float highPass(float current, float gravity) {
            return current - gravity;
        }

        // Deemphasize transient forces
        private float lowPass(float current, float gravity) {
            return gravity * mAlpha + current * (1 - mAlpha);
        }
    }
}
