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
import android.view.View;
import android.widget.TextView;

import com.google.android.things.contrib.driver.motorhat.MotorHat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;


/**
 * Skeleton of an Android Things activity.
 * <p>
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements Nes30Listener {

    static final String TAG = "RobocarBT";
    public static final String NES30_MAC_ADDRESS = "16:08:22:03:A5:D2"; // Robo1
    Nes30Manager nes30Manager;
    Nes30Connection nes30Connection;
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
    private static final int REQUEST_ENABLE_BT = 101;
    private static final int DISCOVERABLE_TIMEOUT_MS = 3000;
    private static final int REQUEST_CODE_ENABLE_DISCOVERABLE = 100;
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

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDiscoverable();
            }
        });

        //startbt();
        // Remote control (for RCDriver)
        setupBluetooth();
        //   mBluetoothAdapter.setName("Robby Car");
    }


    private void setupBluetooth() {
        nes30Manager = new Nes30Manager(this);
        nes30Connection = new Nes30Connection(this, NES30_MAC_ADDRESS);
        logthis("BT status: %b" + " " + nes30Connection.isEnabled());
        logthis("Paired devices: %d" + " " + nes30Connection.getPairedDevices().size());

        BluetoothDevice nes30device = nes30Connection.getSelectedDevice();
        if (nes30device == null) {
            logthis("Starting discovery: %b" + " " + nes30Connection.startDiscovery());
        } else {
            logthis("Creating bond: %b" + " " + nes30Connection.createBond(nes30device));
        }
    }


    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
//        if (getGameControllerIds().isEmpty()) {
//            logthis("no bluetooth starting discovery.");
//            // Register for broadcasts when a device is discovered.
//
//            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//            filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
//            filter.addAction( "android.bluetooth.adapter.action.DISCOVERY_FINISHED");
//            registerReceiver(mReceiver, filter);
//            mBluetoothAdapter.startDiscovery();
//
//        }
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
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                logthis("Found: " + deviceName + " " + deviceHardwareAddress);
//                    BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");


//                device .getClass() .getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
//                device.getClass().getMethod("cancelPairingUserInput", boolean.class).invoke(device);


                device.createBond();
                device.setPairingConfirmation(false);
//                try {
//                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
//                    method.invoke(device, (Object[]) null);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }
    };


    //This code will check to see if there is a bluetooth device and
    //turn it on if is it turned off.
    public void startbt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            logthis("This device does not support bluetooth");
            return;
        }
        //make sure bluetooth is enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            logthis("There is bluetooth, but turned off");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            logthis("The bluetooth is ready to use.");
            //bluetooth is on, so list paired devices from here.
        }
    }

    /**
     * Enable the current {@link BluetoothAdapter} to be discovered (available for pairing) for
     * the next {@link #DISCOVERABLE_TIMEOUT_MS} ms.
     */
    private void enableDiscoverable() {
        Log.d(TAG, "Registering for discovery.");
        Intent discoverableIntent =
            new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
            DISCOVERABLE_TIMEOUT_MS);
        startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_DISCOVERABLE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_DISCOVERABLE) {
            Log.d(TAG, "Enable discoverable returned with result " + resultCode);

            // ResultCode, as described in BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE, is either
            // RESULT_CANCELED or the number of milliseconds that the device will stay in
            // discoverable mode. In a regular Android device, the user will see a popup requesting
            // authorization, and if they cancel, RESULT_CANCELED is returned. In Android Things,
            // on the other hand, the authorization for pairing is always given without user
            // interference, so RESULT_CANCELED should never be returned.
            if (resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Enable discoverable has been cancelled by the user. " +
                    "This should never happen in an Android Things device.");
                return;
            }
            Log.i(TAG, "Bluetooth adapter successfully set to discoverable mode. " +
                "Any A2DP source can find it with the name " + mBluetoothAdapter.getName() +
                " and pair for the next " + DISCOVERABLE_TIMEOUT_MS + " ms. " +
                "Try looking for it on your phone, for example.");

            // There is nothing else required here, since Android framework automatically handles
            // A2DP Sink. Most relevant Bluetooth events, like connection/disconnection, will
            // generate corresponding broadcast intents or profile proxy events that you can
            // listen to and react appropriately.


        }
    }

    //getting the "joystick" or dpad motion.
    @Override
    public boolean onGenericMotionEvent(android.view.MotionEvent motionEvent) {
        float xaxis = 0.0f, yaxis = 0.0f;
        boolean handled = false;

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

      /*
   * Implements Nes30Listener
   */

    @Override
    public void onKeyPress(@Nes30Manager.ButtonCode int keyCode, boolean isDown) {
        logthis("hi from listener");
        switch (keyCode) {
            case Nes30Manager.BUTTON_UP_CODE:
                goForward();
                break;
            case Nes30Manager.BUTTON_DOWN_CODE:
                goBackward();
                break;
            case Nes30Manager.BUTTON_LEFT_CODE:
                turnLeft();
                break;
            case Nes30Manager.BUTTON_RIGHT_CODE:
                turnRight();
                break;
            case Nes30Manager.BUTTON_X_CODE:
                if (isDown) {
                    logthis("Starting camera session for single pics.");
                }
                break;
            case Nes30Manager.BUTTON_Y_CODE:
                logthis("Y button");
                break;
            case Nes30Manager.BUTTON_A_CODE:
                logthis("a button");
                break;
            case Nes30Manager.BUTTON_B_CODE:
                logthis("Button B pressed.");
                break;
            case Nes30Manager.BUTTON_L_CODE:
                logthis("Button L pressed.");
                break;
            case Nes30Manager.BUTTON_R_CODE:
                logthis("Button R pressed.");
                break;
            case Nes30Manager.BUTTON_SELECT_CODE:
                logthis("Select button pressed.");
                break;
            case Nes30Manager.BUTTON_START_CODE:
                logthis("Start button pressed.");
                break;
            case Nes30Manager.BUTTON_KONAMI:
                // Do your magic here ;-)
                logthis("Button KON pressed.");
                break;
        }
    }


    public void logthis(String item) {
        logger.append(item + "\n");
        Log.d(TAG, item);
    }

    //From Google's page on controller-input
    public ArrayList getGameControllerIds() {
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
