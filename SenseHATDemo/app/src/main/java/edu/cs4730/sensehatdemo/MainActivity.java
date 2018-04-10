package edu.cs4730.sensehatdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;


import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.sensehat.LedMatrix;
import com.google.android.things.contrib.driver.sensehat.SenseHat;


import java.io.IOException;

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
@SuppressLint("all")
public class MainActivity extends Activity {
    final String TAG = MainActivity.class.getSimpleName();
    Bitmap bitmapD, bitmapS;  //led display and one for the screen.
    Canvas canvasD, canvasS;  //same idea.
    Paint paint;
    ImageView iv;
    Spinner mySpinner;
    int matrix[][];
    int displaysize = 30;
    int colorindex = 0;

    //hardware
    LedMatrix display;

    ColorList myColor = new ColorList();

    Bmx280 mBmx280;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //need a LSM9DS1 driver for this.  we don't have one yet.
  /*      try {
            mBmx280 = new Bmx280("I2C5");//find the right one....
            mBmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            float temperature = mBmx280.readTemperature();
            Log.v(TAG, "Temperature is " + temperature);
        } catch (IOException e) {
            Log.wtf(TAG, "no temperature sensor!");
        }
*/


        iv = findViewById(R.id.display);
        mySpinner = findViewById(R.id.spinner);

        String name[] = new String[23];
        for (int i = 0; i < 23; i++) {
            name[i] = myColor.getName();
            myColor.next();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, name);
        //set the dropdown layout
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //finally set the adapter to the spinner
        mySpinner.setAdapter(adapter);
        //set the selected listener as well
        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                colorindex = myColor.getColorbyIndex(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                colorindex = 23;
            }
        });

        //led matrix
        bitmapD = Bitmap.createBitmap(SenseHat.DISPLAY_WIDTH, SenseHat.DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
        canvasD = new Canvas(bitmapD);
        //screen version
        bitmapS = Bitmap.createBitmap(SenseHat.DISPLAY_WIDTH * displaysize, SenseHat.DISPLAY_HEIGHT * displaysize, Bitmap.Config.ARGB_8888);
        canvasS = new Canvas(bitmapS);
        paint = new Paint();

        try {
            // Color the LED matrix.
            display = SenseHat.openDisplay();
        } catch (IOException e) {
            e.printStackTrace();
            display = null;
        }
        matrix = new int[SenseHat.DISPLAY_WIDTH][SenseHat.DISPLAY_HEIGHT];

        Log.d(TAG, "w and h is " + SenseHat.DISPLAY_HEIGHT);

        displayLED();
        displaybmp();

        //lastly set the image view listener

        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //We just need the x and y position, to draw on the canvas
                //so, retrieve the new x and y touch positions
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        return v.performClick();
                    case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        if (x < displaysize) x = 0;
                        else x = (int) (x - displaysize) / displaysize + 1;
                        if (y < displaysize) y = 0;
                        else y = (int) (event.getY() - displaysize) / displaysize + 1;
                        Log.d(TAG, "x is " + event.getX() + " mod is " + x);
                        if (x >= SenseHat.DISPLAY_WIDTH || y >= SenseHat.DISPLAY_HEIGHT)
                            return false;
                        matrix[x][y] = colorindex;
                        displaybmp();
                        displayLED();
                        return true;
                }
                return false;
            }
        });
    }

    private void displaybmp() {
        //create the display
        canvasS.drawColor(Color.BLACK);
        int incr = displaysize;
        for (int i = 0; i < SenseHat.DISPLAY_WIDTH; i++)
            for (int j = 0; j < SenseHat.DISPLAY_HEIGHT; j++) {
                paint.setColor(matrix[i][j]);
                // paint.setColor(Color.RED);
                canvasS.drawRect(i * incr, j * incr, (i * incr) + incr - 1, j * incr + incr - 1, paint);
                //canvasD.drawPoint(i, j, paint);
            }

        //show it
        iv.setImageBitmap(bitmapS);
    }

    private void displayLED() {
        if (display == null) return;

        for (int i = 0; i < SenseHat.DISPLAY_WIDTH; i++)
            for (int j = 0; j < SenseHat.DISPLAY_HEIGHT; j++) {
                paint.setColor(matrix[i][j]);
                canvasD.drawPoint(i, j, paint);
            }


        try {
            //display.draw(Color.MAGENTA);
            //display.draw(getDrawable(android.R.drawable.ic_dialog_alert));
            // Display a gradient on the LED matrix.
            display.draw(bitmapD);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
