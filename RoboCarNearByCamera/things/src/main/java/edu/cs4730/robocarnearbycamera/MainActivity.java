package edu.cs4730.robocarnearbycamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.things.contrib.driver.motorhat.MotorHat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private CameraHandler mCameraHandler;
    /**
     * Camera image capture size
     */
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;

    //for the car
    roboCar car;

    //Nearby stuff
    NearByMgr nearByMgr;
    Thread myThread;

    static String TAG = "MainActivity";
    ImageView iv_cam;
    String ConnectedEndPointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_cam = findViewById(R.id.iv_cam);
        try {
            car = new roboCar(new MotorHat(BoardDefaults.getI2cBus()), null);
            Log.d(TAG, "car started!");
        } catch (IOException e) {
            Log.d(TAG, "Motor Hat NOT found!!!!!!\n");
            e.printStackTrace();
            car = null;
        }
        initCamera();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPhoto();

            }
        });

        nearByMgr = new NearByMgr(this, new NearByMgr.OnNearByCallback() {
            @Override
            public void onConnectionStatus(int Status) {
                //once connected, kick start the picture sending.
                if (Status == ConnectionsStatusCodes.STATUS_OK) {
                    //start sending pic
                    sendPictures();
                    ConnectedEndPointId = "something";
                } else {
                    //issue all stop just in case.
                    if (car != null)
                        car.allstop();
                    // if -1, then connect died, and stop sending.  same for all of them, start advert again.
                    //stop sending pict's.
                    ConnectedEndPointId = "";
                        //should auto stop...
                    //and startup the advertising again.
                    nearByMgr.startAdvertising();
                }
            }

            @Override
            public void onDataBytes(String Data) {
                switch (Data) {
                    case "S":  //stop
                        car.allstop();
                        break;
                    case "F": //forward
                        car.goForward();
                        break;
                    case "B": //backward
                        car.goBackward();
                        break;
                    case "L": //turn left
                        car.turnLeft();
                        break;
                    case "R":  //turn right
                        car.turnRight();
                        break;
                    default:
                        Log.wtf(TAG, "unknown command" + Data);
                }
            }
        });
        nearByMgr.startAdvertising();
    }

    /**
     * Initialize the camera that will be used to capture images.
     */
    private void initCamera() {

        mCameraHandler = CameraHandler.getInstance();
        mCameraHandler.initializeCamera(this,
            PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, null,
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    // iv_cam.set  imageReader.acquireNextImage());
                   // Log.wtf(TAG, "got a picture, attempting to display");
                    Image image = imageReader.acquireLatestImage();
                                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    iv_cam.setImageBitmap(bitmapImage);
                    InputStream targetStream = new ByteArrayInputStream(bytes);
                    nearByMgr.sendStream(targetStream);
                    //  mCameraHandler.resetCamera();
                    closeCamera();  //this is a hack that needs fixed.
                    initCamera();
                    synchronized(myThread) {
                        Log.d(TAG, "waiting up thread to take next pic.");
                        myThread.notify(); //in theory, this should wakeup the thread.  OR myThread.notifyAll()
                    }

                }
            });
    }

    /**
     * Clean up resources used by the camera.
     */
    private void closeCamera() {
        mCameraHandler.shutDown();
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method {@link # onPhotoReady(Bitmap)} must be called with the image.
     */
    private void loadPhoto() {
        mCameraHandler.takePicture(null);
    }


    void sendPictures() {
        myThread = new Thread(new sendPics(1000));  //
        //so going have to be a thread or async task or this will overload the main activity.

        myThread.start();


    }


    /**
     * The job of this thread is send pictures to the phone, so they can see where it going.
     * We can't send a media stream, so this is faking it...
     */
    class sendPics implements Runnable {
        int sleeptime = 250;

        sendPics(int sleeptime) {
            this.sleeptime = sleeptime;
        }

        @Override
        public void run() {
            int i = 0;
            HandlerThread thread = new HandlerThread("takepics");
            thread.start();
            while (ConnectedEndPointId.compareTo("") != 0) {
                Log.d(TAG, "i is " + i);
                // get an image from the camera

                mCameraHandler.takePicture(new Handler(thread.getLooper()));
                try {
                    synchronized (myThread) {
                        myThread.wait();
                    }
                    //Thread.sleep(sleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //we should not need this, but... not working.
                try {
                    Thread.sleep(sleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
                if (i == 4) i = 0;  //4 pictures a second?

            }
            Log.d(TAG, "Leaving thread!");
        }


    }

}
