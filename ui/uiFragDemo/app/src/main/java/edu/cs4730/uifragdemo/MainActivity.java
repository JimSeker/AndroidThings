package edu.cs4730.uifragdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 *
 */
public class MainActivity extends Activity {

    boolean firstfragment = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //if this a not new, then place add firstfragment to the framelayout
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                .add(R.id.container, new oneFragment())
                .commit();
        }

        //find the button and setup the listener.
        Button btn1 = (Button) findViewById(R.id.button01);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstfragment) {
                    //first fragment is showing, so replace it with the second one.
                    getFragmentManager().beginTransaction()
                        .replace(R.id.container, new twoFragment())
                        .commit();
                    firstfragment = false;
                } else {
                    //second fragment is showing, so replace it with the second one.
                    getFragmentManager().beginTransaction()
                        .replace(R.id.container, new oneFragment())
                        .commit();
                    firstfragment = true;

                }
            }
        });
    }
}
