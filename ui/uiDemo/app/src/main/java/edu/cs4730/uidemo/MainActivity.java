package edu.cs4730.uidemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 *  This is an example using the UI.  This assumes the device has a monitor plugged in, otherwise, nothing happens.
 *
 *  This example does nothing with the hardware.  No HAT is required for this example.
 */
public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener, TextWatcher,
    Button.OnClickListener {

    //variables for the widgets
    RadioGroup myRadioGroup;
    EditText et;
    Button btnalert;
    TextView label;

    //variable for the log
    String TAG = "ForExample";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //EditText view setup and listner
        et =  findViewById(R.id.ETname);
        et.addTextChangedListener(this);

        //the top label in the xml doc.
        label =  findViewById(R.id.Label01);

        //setup the radio group with a listener.
        myRadioGroup = findViewById(R.id.SndGroup);
        myRadioGroup.setOnCheckedChangeListener(this);

        //setup the button with a listener as well.
        btnalert =  findViewById(R.id.Button01);
        btnalert.setOnClickListener(this);


    }

    /*  Radio group listener for OnCheckedChangeListener */
    public void onCheckedChanged(RadioGroup group, int CheckedId) {
        if (group == myRadioGroup) { //if not myRadioGroup, we are in trouble!
            if (CheckedId == R.id.RB01) {
                // information radio button clicked
                Log.d(TAG, "RB01 was pushed.");
            } else if (CheckedId == R.id.RB02) {
                // Confirmation radio button clicked
                Log.d(TAG, "RB02 was pushed.");
            } else if (CheckedId == R.id.RB03) {
                // Warning radio button clicked
                Toast.makeText(this, "Warning!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /* EditView listeners */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (et.length() > 10) {
            Toast.makeText(getApplicationContext(), "Long Word!", Toast.LENGTH_SHORT).show();  //currently toasts are not showing.
            Log.d(TAG, "Long Word!");
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //left blank
    }

    public void afterTextChanged(Editable s) {
        //left blank
    }

    /* button listener */
    public void onClick(View v) {
        if (v == btnalert) {
            showlistdialog("Dialog Box test");
            Toast.makeText(getApplicationContext(), "The button was pressed", Toast.LENGTH_SHORT).show();  //curently not showing.
            Log.d(TAG, "The button was pressed.");
        }

    }


    /*
 * This shows a list and the use is to select one of them.
 * Note, this dialog doesn't set a cancel listener, like the one above.  so if the user
 * cancels, nothing happens.
 */
    void showlistdialog(String title) {
        logthis("dialog called.");
        //Note, the dialog needs to be dark, because the background is white and at least right now, you can't see the dialog or the buttons.
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(title);
        builder.setMessage("Play again?");
        //Button Button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logthis("play again");

            }
        });
        //Negative button
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logthis("exit");

            }
        });
        //If the user uses the back button instead.
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                logthis("canceled ");

            }
        });
        builder.show();
        logthis("dialog should be showing.");
    }

    //simple helper function to display to logger and log.d as well.
    public void logthis(String item) {
       // logger.append(item + "\n");
        Log.d(TAG, item);
    }

}
