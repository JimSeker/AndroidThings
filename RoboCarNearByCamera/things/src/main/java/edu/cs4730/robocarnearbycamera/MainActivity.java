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

import java.io.File;
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
    /** Camera image capture size */
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;

    static String TAG = "MainActivity";
    ImageView iv_cam;

    Camera2Preview mPreview;
    FrameLayout preview;
    //for taking a picture.
    Camera2CapturePic mCapture;
    Thread myThread;


    String ConnectedEndPointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_cam = findViewById(R.id.iv_cam);

        initCamera();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPhoto();
           //     sendPictures();
            }
        });

//        //camera stuff
//        preview = (FrameLayout) findViewById(R.id.camera2_preview);
//
//        //we have to pass the camera id that we want to use to the surfaceview
//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        try {
//            String cameraId = manager.getCameraIdList()[0];
//            mPreview = new Camera2Preview(this, cameraId);
//            preview.addView(mPreview);
//
//        } catch (CameraAccessException e) {
//            Log.v(TAG, "Failed to get a camera ID!");
//            e.printStackTrace();
//        }
//


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
                    Log.wtf(TAG, "got a picture, attempting to display");
                    Image image = imageReader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                   iv_cam.setImageBitmap(bitmapImage);
                  //  mCameraHandler.resetCamera();
                   closeCamera();  //this is a hack that needs fixed.
                    initCamera();

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
        mCameraHandler.takePicture();
    }

    void sendPictures() {
        myThread = new Thread(new sendPics(250));
        //so going have to be a thread or async task or this will overload the main activity.
        if (mCapture == null) // While I would like the declare this earlier, the camera is not setup yet, so wait until now.
            mCapture = new Camera2CapturePic(this, mPreview);
        mCapture.setThread(myThread);
        myThread.start();



    }

    class sendPics implements Runnable {
        int sleeptime =250;
        sendPics (int sleeptime) {
            this.sleeptime = sleeptime;
        }

        @Override
        public void run() {
            int i = 0;
            File mediaFile;
            File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return;
                }
            }
            ConnectedEndPointId = "something";
            while (ConnectedEndPointId != "") {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + i + ".jpg");
                Log.d(TAG, "File is " + mediaFile.getAbsolutePath());
                // get an image from the camera
                if (mCapture.reader != null) {  //I'm sure it's setup correctly if reader is not null.
                    mCapture.TakePicture(mediaFile);
                    try {
                        synchronized (myThread) {
                            myThread.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "pic taken, now sending.");
                   // send(mediaFile);
                    Log.d(TAG, "Send successful");
                }
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
