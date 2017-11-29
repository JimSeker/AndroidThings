package edu.cs4730.weatherstationrainbowhatonly;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;

/**
 * This is based on the Android Things WeatherStation that android created, but rewritten to
 * use only the raspberry Pi and RainBow HAT piece.
 * <p>
 * Rainbow hat doc's at https://github.com/androidthings/contrib-drivers/tree/master/rainbowhat
 * Original example is https://github.com/androidthings/weatherstation
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private enum DisplayMode {
        TEMPERATURE,
        PRESSURE
    }

    private DisplayMode mDisplayMode = DisplayMode.TEMPERATURE;

    private SensorManager mSensorManager;

    private ButtonInputDriver mButtonA, mButtonB, mButtonC;

    private Bmx280SensorDriver mEnvironmentalSensorDriver;

    private AlphanumericDisplay mDisplay;
    //the lights above the buttons.
    private Gpio mLedR, mLedG, mLedB;
    //light strip/ rainbow lights.
    private Apa102 mLedstrip;
    private int[] mRainbow = new int[7];
    private static final int LEDSTRIP_BRIGHTNESS = 1;


    private int SPEAKER_READY_DELAY_MS = 300;
    private Speaker mSpeaker;

    private float mLastTemperature;
    private float mLastPressure;

    TextView ButtonA, ButtonB, ButtonC, tv_ledstrip, tv_temp, tv_pressure, logger;


    // Callback used when we register the BMP280 sensor driver with the system's SensorManager.
    private SensorManager.DynamicSensorCallback mDynamicSensorCallback
        = new SensorManager.DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            Log.d(TAG, "At least one sensor exists?");
            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                // Our sensor is connected. Start receiving temperature data.
                Log.d(TAG, "registering temp");
                mSensorManager.registerListener(mTemperatureListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Our sensor is connected. Start receiving pressure data.
                Log.d(TAG, "registering pressure");
                mSensorManager.registerListener(mPressureListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
        }
    };

    // Callback when SensorManager delivers temperature data.
    private SensorEventListener mTemperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastTemperature = (event.values[0] * 1.8F) + 32.0F;
            //mLastTemperature = event.values[0];
            //  Log.d(TAG, "temperature sensor changed: " + mLastTemperature);
            if (mDisplayMode == DisplayMode.TEMPERATURE) {
                updateDisplay(mLastTemperature);
            }
            tv_temp.setText(String.valueOf(mLastTemperature));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    // Callback when SensorManager delivers pressure data.
    private SensorEventListener mPressureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastPressure = event.values[0];
            //   Log.d(TAG, "sensor changed: " + mLastPressure);
            if (mDisplayMode == DisplayMode.PRESSURE) {
                updateDisplay(mLastPressure);
            }
            tv_pressure.setText(String.valueOf(mLastPressure));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Started App");
        logger = findViewById(R.id.logger);

        //textviews for the display (assuming anyone can view it.
        ButtonA = findViewById(R.id.tv_buttona);
        ButtonA.setText("Off");
        ButtonB = findViewById(R.id.tv_buttonb);
        ButtonB.setText("Off");
        ButtonC = findViewById(R.id.tv_buttonc);
        ButtonC.setText("Off");

        tv_ledstrip = findViewById(R.id.tv_ledstrip);
        tv_ledstrip.setText("Off");
        tv_temp = findViewById(R.id.tv_temp);
        tv_pressure = findViewById(R.id.tv_press);

        //first get the sensor manager.
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        // setup the three GPIO buttons, so they generate a key press that can be handled.
        try {
            mButtonA = RainbowHat.createButtonAInputDriver(KeyEvent.KEYCODE_A);
            mButtonA.register();

            mButtonB = RainbowHat.createButtonBInputDriver(KeyEvent.KEYCODE_B);
            mButtonB.register();

            mButtonC = RainbowHat.createButtonCInputDriver(KeyEvent.KEYCODE_C);
            mButtonC.register();

            logthis("Initialized GPIO Buttons that generates a keypress with KEYCODE_A, B, and C", false);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing GPIO button", e);
        }
        // Continuously report temperature and pressure.
        try {
            mEnvironmentalSensorDriver = RainbowHat.createSensorDriver();
            mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
            mEnvironmentalSensorDriver.registerTemperatureSensor();
            mEnvironmentalSensorDriver.registerPressureSensor();

            logthis("Initialized I2C BMP280", false);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing BMP280", e);
        }

        //get the display
        try {
            mDisplay = RainbowHat.openDisplay();
           // mDisplay.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            mDisplay.setEnabled(true);
            mDisplay.clear();
            logthis("Initialized I2C Display", false);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing display", e);
            logthis("Display disabled", true);
            mDisplay = null;
        }

        // SPI ledstrip
        try {
            mLedstrip = RainbowHat.openLedStrip();
            mLedstrip.setBrightness(LEDSTRIP_BRIGHTNESS);
            for (int i = 0; i < mRainbow.length; i++) {
                float[] hsv = {i * 360.f / mRainbow.length, 1.0f, 1.0f};
                mRainbow[i] = Color.HSVToColor(255, hsv);
            }
            logthis("initialized ledstrip", false);
        } catch (IOException e) {
            Log.e(TAG, "ledstrip disabled" + e);
            logthis("ledstrip disabled", true);
            mLedstrip = null; // Led strip is optional.
            tv_ledstrip.setText("Failed to register");
        }

        // GPIO led
        try {
            mLedR = RainbowHat.openLedRed();
            mLedB = RainbowHat.openLedBlue();
            mLedG = RainbowHat.openLedGreen();
            logthis("initialized led G, R, and B", false);
            //do I need these?  don't seem do change anything.
//            mLedR.setEdgeTriggerType(Gpio.EDGE_NONE);
//            mLedR.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            mLedR.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing led", e);
        }

        // PWM speaker

        try {
            mSpeaker = RainbowHat.openPiezo();
            logthis("initialized speaker", false);
            playSound();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing speaker", e);
        }

        //display  button A, B, and C
        findViewById(R.id.btn_a).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateLeds(KeyEvent.KEYCODE_A, true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        updateLeds(KeyEvent.KEYCODE_A, false);
                        return true;
                }
                return false;
            }
        });
        findViewById(R.id.btn_b).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateLeds(KeyEvent.KEYCODE_B, true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        updateLeds(KeyEvent.KEYCODE_B, false);
                        return true;
                }
                return false;
            }
        });

        findViewById(R.id.btn_c).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateLeds(KeyEvent.KEYCODE_C, true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        updateLeds(KeyEvent.KEYCODE_C, false);
                        return true;
                }
                return false;
            }
        });
        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            updateLeds(keyCode, true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_B) {
            updateLeds(keyCode, true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_C) {
            updateLeds(keyCode, true);
            return true;
        }
        logthis("A unknown keycode was called." + keyCode, true);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            updateLeds(keyCode, false);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_B) {
            updateLeds(keyCode, false);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_C) {
            updateLeds(keyCode, false);
            return true;
        }
        logthis("A unknown keycode was called." + keyCode, true);
        return super.onKeyUp(keyCode, event);
    }

    private void updateLeds(int keyCode, boolean down) {
        if (down) {
            if (keyCode == KeyEvent.KEYCODE_A) {
                mDisplayMode = DisplayMode.PRESSURE;
                updateDisplay(mLastPressure);

                try {
                    mLedR.setValue(true);
                    ButtonA.setText("On");
                } catch (IOException e) {
                    ButtonA.setText("Error");
                    logthis("Error update LED R, button A " + e.getMessage(), true);
                }
            } else if (keyCode == KeyEvent.KEYCODE_B) {
                try {
                    mLedG.setValue(true);
                    mLedstrip.write(mRainbow);
                    ButtonB.setText("On");
                    tv_ledstrip.setText("On");
                } catch (IOException e) {
                    ButtonB.setText("Error?");
                    tv_ledstrip.setText("Error?");
                    logthis("Error update LED G, button B or ledstrip " + e.getMessage(), true);
                }
            } else if (keyCode == KeyEvent.KEYCODE_C) {
                try {
                    ButtonC.setText("On");
                    mLedB.setValue(true);
                } catch (IOException e) {
                    ButtonC.setText("Error");
                    logthis("Error update LED B, button C " + e.getMessage(), true);
                }
                playSound();
            }
        } else {  //up button
            if (keyCode == KeyEvent.KEYCODE_A) {
                mDisplayMode = DisplayMode.TEMPERATURE;
                updateDisplay(mLastTemperature);
                try {
                    mLedR.setValue(false);
                    ButtonA.setText("Off");
                } catch (IOException e) {
                    ButtonA.setText("Error");
                    logthis("Error update LED R, button A " + e.getMessage(), true);
                }
            } else if (keyCode == KeyEvent.KEYCODE_B) {
                try {
                    mLedG.setValue(false);
                    mLedstrip.write(new int[7]);  //turn it off.
                    ButtonB.setText("Off");
                    tv_ledstrip.setText("Off");
                } catch (IOException e) {
                    logthis("Error update LED G, button B or ledstrip " + e.getMessage(), true);
                    ButtonB.setText("Error?");
                    tv_ledstrip.setText("Error?");
                }
            } else if (keyCode == KeyEvent.KEYCODE_C) {
                try {
                    mLedB.setValue(false);
                    ButtonC.setText("Off");
                } catch (IOException e) {
                    ButtonC.setText("Error");
                    logthis("Error update LED B, button C " + e.getMessage(), true);
                }
            }
        }
    }

    private void updateDisplay(float value) {
        if (mDisplay != null) {
            try {
                mDisplay.display(value);
            } catch (IOException e) {
                logthis("Error setting display " + e.getMessage(), true);
            }
        }
    }


    private void playSound() {
        logthis("Playing sound", false);
        final ValueAnimator slide = ValueAnimator.ofFloat(440, 440 * 4);
        slide.setDuration(50);
        slide.setRepeatCount(5);
        slide.setInterpolator(new LinearInterpolator());
        slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                try {
                    float v = (float) animation.getAnimatedValue();
                    mSpeaker.play(v);  //this is how to play a sound.  everything else is for the effect.
                } catch (IOException e) {
                    throw new RuntimeException("Error sliding speaker", e);
                }
            }
        });
        slide.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    mSpeaker.stop();
                } catch (IOException e) {
                    throw new RuntimeException("Error sliding speaker", e);
                }
            }
        });
        Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                slide.start();
            }
        }, SPEAKER_READY_DELAY_MS);
    }

    void logthis(String item, boolean error) {
        if (error) {
            Log.e(TAG, item);
        } else {
            Log.d(TAG, item);
        }
        logger.append(item + "\n");
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Clean up sensor registrations
        mEnvironmentalSensorDriver.unregisterTemperatureSensor();
        mEnvironmentalSensorDriver.unregisterPressureSensor();
        //mSensorManager.unregisterListener(mTemperatureListener);
        //mSensorManager.unregisterListener(mPressureListener);
        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);

        // Clean up peripheral.
        if (mEnvironmentalSensorDriver != null) {
            try {
                mEnvironmentalSensorDriver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEnvironmentalSensorDriver = null;
        }
        //remove buttons
        if (mButtonA != null) {
            try {
                mButtonA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonA = null;
        }
        if (mButtonB != null) {
            try {
                mButtonB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonB = null;
        }
        if (mButtonC != null) {
            try {
                mButtonC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonC = null;
        }
        if (mDisplay != null) {
            try {
                mDisplay.clear();
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling display", e);
            } finally {
                mDisplay = null;
            }
        }

        if (mLedstrip != null) {
            try {
                mLedstrip.setBrightness(0);
                mLedstrip.write(new int[7]);
                mLedstrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling ledstrip", e);
            } finally {
                mLedstrip = null;
            }
        }

        if (mLedR != null) {
            try {
                mLedR.setValue(false);
                mLedR.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling ledR", e);
            } finally {
                mLedR = null;
            }
        }
        if (mLedG != null) {
            try {
                mLedG.setValue(false);
                mLedG.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling ledG", e);
            } finally {
                mLedG = null;
            }
        }
        if (mLedB != null) {
            try {
                mLedB.setValue(false);
                mLedB.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling ledB", e);
            } finally {
                mLedB = null;
            }
        }
        Log.d(TAG, "Everything should be unregistered and turned off");
    }
}
