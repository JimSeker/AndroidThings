package edu.cs4730.robocarnearbycamera;

import android.util.Log;
import android.widget.TextView;
import com.google.android.things.contrib.driver.motorhat.MotorHat;
import java.io.IOException;

/**
 * This is a class to hold the movement functions of the car.
 * It should make the code in MainActivity a lot simpler.
 */

public class roboCar {

    static final String TAG = "roboCar";
    TextView logger = null;

    boolean initialized = false;

    //car and motor hat variables.
    private MotorHat mMotorHat = null;
    private static final int[] ALL_MOTORS = {0, 1, 2, 3};
    private static final int[] LEFT_MOTORS = {2, 3};
    private static final int[] RIGHT_MOTORS = {0, 1};

    private static final int SPEED_NORMAL = 100;
    private static final int SPEED_TURNING_INSIDE = 70;
    private static final int SPEED_TURNING_OUTSIDE = 250;

    roboCar() {
        initialized = false;
    }

    roboCar( MotorHat m, TextView log) {
        if (m != null) {
            mMotorHat = m;
            initialized = true;
            logthis("motorhat initialized and ready");
        } else {
            initialized = false;
            logthis("motorhat initialized failed.");
        }
        logger = log;
    }

    public void initialize( MotorHat m, TextView log) {
        if (m != null) {
            mMotorHat = m;
            initialized = true;
            logthis("motorhat initialized and ready");
        } else {
            initialized = false;
            logthis("motorhat initialized failed.");
        }
        logger = log;
    }

    public void allstop() {
        try {

            for (int motor : ALL_MOTORS) {
                mMotorHat.setMotorState(motor, MotorHat.MOTOR_STATE_RELEASE);
            }
            logthis("Set everything to stop");
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);
        }
    }

    public void goForward() {
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

    public void goBackward() {
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

    public void turnLeft() {
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

    public void turnRight() {
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
            logthis("set everything for Right");
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);

        }
    }


    private void logthis(String item) {
        Log.d(TAG, item);

        if (logger != null)   logger.append(item + "\n");

    }
}
