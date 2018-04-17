package edu.cs4730.robocarbtcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.things.bluetooth.BluetoothConnectionManager;
import com.google.android.things.bluetooth.BluetoothPairingCallback;
import com.google.android.things.bluetooth.PairingParams;
import com.google.android.things.contrib.driver.motorhat.MotorHat;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Skeleton of an Android Things activity.
 * <p>
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 * need to rewrite the bluetooth for the dev7 https://developer.android.com/things/sdk/apis/bluetooth.html pairing.
 */
public class MainActivity extends Activity {

    static final String TAG = "RobocarBT";

    //car and motor hat variables.
    private MotorHat mMotorHat;
    private static final int[] ALL_MOTORS = {0, 1, 2, 3};
    private static final int[] LEFT_MOTORS = {2, 3};
    private static final int[] RIGHT_MOTORS = {0, 1};

    private static final int SPEED_NORMAL = 100;
    private static final int SPEED_TURNING_INSIDE = 70;
    private static final int SPEED_TURNING_OUTSIDE = 250;

    //for bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    public static final String BOOTHTOOTH_MAC_ADDRESS = "16:08:22:03:A5:D2"; // Robo1
    BluetoothConnectionManager mBluetoothConnectionManager;


    //rest fo the variables;
    TextView logger;
    Boolean isJoyStick = false, isGamePad = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logger = findViewById(R.id.logger);
        try {
            mMotorHat = new MotorHat(BoardDefaults.getI2cBus());
            logthis("found Motor Hat\n");
        } catch (IOException e) {
            logthis("Motor Hat NOT found!!!!!!\n");
            throw new RuntimeException("Failed to create MotorHat", e);
        }

        //If we need to find a bluetooth controller.
        mBluetoothConnectionManager = BluetoothConnectionManager.getInstance();
        mBluetoothConnectionManager.registerPairingCallback(mBluetoothPairingCallback);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    private BluetoothPairingCallback mBluetoothPairingCallback = new BluetoothPairingCallback() {

        @Override
        public void onPairingInitiated(BluetoothDevice bluetoothDevice,
                                       PairingParams pairingParams) {
            // Handle incoming pairing request or confirmation of outgoing pairing request
           // handlePairingRequest(bluetoothDevice, pairingParams);
            Log.wtf(TAG, "onpairing to device" + bluetoothDevice.getAddress());

            mBluetoothConnectionManager.finishPairing(bluetoothDevice);
        }

        @Override
        public void onPaired(BluetoothDevice bluetoothDevice) {
            // Device pairing complete
            Log.wtf(TAG, "paired to device" + bluetoothDevice.getAddress());
        }

        @Override
        public void onUnpaired(BluetoothDevice bluetoothDevice) {
            // Device unpaired
            Log.wtf(TAG, "UNpaired device" + bluetoothDevice.getAddress());
        }

        @Override
        public void onPairingError(BluetoothDevice bluetoothDevice,
                                   BluetoothPairingCallback.PairingError pairingError) {
            // Something went wrong!
            Log.wtf(TAG, "Error with device" + bluetoothDevice.getAddress());
            Log.wtf(TAG, pairingError.toString());
        }
    };

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        if (getGameControllerIds().isEmpty()) {
            logthis("no bluetooth starting discovery.");
            // Register for broadcasts when a device is discovered.
//
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
            filter.addAction( "android.bluetooth.adapter.action.DISCOVERY_FINISHED");
            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
        } else {
            logthis("found bluetooth device.");
        }
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logthis("found something");
            if (action.equals("android.bluetooth.adapter.action.DISCOVERY_STARTED")) {
                logthis("Discovery has started");
            } else if (action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")) {
                logthis("Discovery has ended");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = remoteDevice.getName();
                String deviceHardwareAddress = remoteDevice.getAddress(); // MAC address
                logthis("Found: " + deviceName + " " + deviceHardwareAddress);
                if ( deviceHardwareAddress.compareTo(BOOTHTOOTH_MAC_ADDRESS) ==0) {  //my controller
                    logthis("connecting to bluetooth device");
                    mBluetoothConnectionManager.initiatePairing(remoteDevice);

                    mBluetoothConnectionManager.connect(remoteDevice);
                    getGameControllerIds();
                }
            }
        }
    };


    //getting the "joystick" or dpad motion.
    @Override
    public boolean onGenericMotionEvent(android.view.MotionEvent motionEvent) {
        float xaxis = 0.0f, yaxis = 0.0f;
        boolean handled = false;
        logthis("motion event");
        //if both are true, this code will show both JoyStick and dpad.  Which one you want to use is
        // up to you
        if (isJoyStick) {
            xaxis = motionEvent.getAxisValue(MotionEvent.AXIS_X);
            yaxis = motionEvent.getAxisValue(MotionEvent.AXIS_Y);

            logthis("JoyStick: X " + xaxis + " Y " + yaxis + "\n");
            handled = true;
        }

        if (isGamePad) {
            xaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X);
            yaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y);

            // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
            // LEFT and RIGHT direction accordingly.
            if (Float.compare(xaxis, -1.0f) == 0) {
                // Dpad.LEFT;
                logthis("Dpad Left");
                turnLeft();
                handled = true;
            } else if (Float.compare(xaxis, 1.0f) == 0) {
                // Dpad.RIGHT;
                logthis("Dpad Right");
                turnRight();
                handled = true;
            }
            // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
            // UP and DOWN direction accordingly.
            else if (Float.compare(yaxis, -1.0f) == 0) {
                // Dpad.UP;
                logthis("Dpad Up");
                goForward();
                handled = true;
            } else if (Float.compare(yaxis, 1.0f) == 0) {
                // Dpad.DOWN;
                logthis("Dpad Down");
                goBackward();
                handled = true;
            } else if ((Float.compare(xaxis, 0.0f) == 0)
                && (Float.compare(yaxis, 0.0f) == 0)) {
                //Dpad.center
                logthis("Dpad centered");
                handled = true;
            }
            if (!handled) {
                logthis("Unknown");
                logger.append("unhandled: X " + xaxis + " Y " + yaxis + "\n");
            }

        }
        return handled;
    }

    //getting the buttons.  note, there is down and up action.  this only
    //looks for down actions.
    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        boolean handled = false;
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
            == InputDevice.SOURCE_GAMEPAD) {

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_BUTTON_X:
                        logthis("X Button");
                        allstop();
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_BUTTON_A:
                        logthis("A Button");
                        allstop();
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_BUTTON_Y:
                        logthis("Y Button");
                        allstop();
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_BUTTON_B:
                        logthis("B Button");
                        allstop();
                        handled = true;
                        break;
                    case 87:
                        logthis("up Button");
                        goForward();
                        handled = true;
                        break;
                    case 88:
                        logthis("Down Button");
                        goBackward();
                        handled = true;
                        break;
                    case 89:
                        logthis("right Button");
                        turnRight();
                        handled = true;
                        break;
                    case 90:
                        logthis("up Button");
                        turnLeft();
                        handled = true;
                        break;
                    case 4:
                    case 24:
                    case 25:
                    case 66:
                        logthis("B Button");
                        allstop();
                        handled = true;
                        break;

                }
                if (!handled)
                    logger.append("code is " + event.getKeyCode() + "\n");
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                //don't care, but need to handle it.
                handled = true;
            } else {
                logger.append("unknown action " + event.getAction());
            }
        }

        return handled;
    }



    public void logthis(String item) {
        logger.append(item + "\n");
        Log.d(TAG, item);
    }

    //From Google's page on controller-input
    public ArrayList getGameControllerIds() {
        logthis("Checking for controllers.");
        ArrayList gameControllerDeviceIds = new ArrayList();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                logger.append("found" + dev.getName());
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
                //possible both maybe true.
                if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    isGamePad = true;
                if ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
                    isJoyStick = true;
                logger.append("GamePad: " + isGamePad + "\n");
                logger.append("JoyStick: " + isJoyStick + "\n");
            }

        }
        return gameControllerDeviceIds;
    }

    private void allstop() {
        try {

            for (int motor : ALL_MOTORS) {
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_RELEASE);
            }
            logthis("Set everything to stop");
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);
        }
    }

    private void goForward() {
        //for each motor, set the speed and direction(state) of the motor.
        try {
            for (int motor : ALL_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_NORMAL);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CW);  //forward
            }
            logthis("Set everything to go forward");
        } catch (IOException e) {
            Log.e(TAG, "Error setting speed or state", e);
        }
    }

    private void goBackward() {
        //for each motor, set the speed and direction(state) of the motor.
        try {
            for (int motor : ALL_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_NORMAL);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CCW);  //backward
            }
            logthis("Set everything to go backward");
        } catch (IOException e) {
            Log.e(TAG, "Error setting speed or state", e);
        }
    }

    private void turnLeft() {
        try {
            //inside motors to turn left
            for (int motor : LEFT_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_INSIDE);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CW);  //forward
            }
            //outside motors to turn left
            for (int motor : RIGHT_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_OUTSIDE);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CW);  //forward
            }
            logthis("set everything for Left");
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);

        }
    }

    private void turnRight() {
        try {
            //inside motors to turn right.
            for (int motor : RIGHT_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_INSIDE);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CW);  //forward
            }
            //outside motors to turn right
            for (int motor : LEFT_MOTORS) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_OUTSIDE);
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_CW);  //forward
            }
            logthis("set everything for Left");
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);

        }
    }
}
