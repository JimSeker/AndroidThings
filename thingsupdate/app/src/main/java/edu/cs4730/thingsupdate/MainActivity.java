package edu.cs4730.thingsupdate;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.update.StatusListener;
import com.google.android.things.update.UpdateManager;
import com.google.android.things.update.UpdateManagerStatus;
import com.google.android.things.update.UpdatePolicy;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class

 *
 * This is a very simple example to force an update of the software.  Since the new dp8 requires app to be HOME, I can't
 * get to the launcher anymore to force an update when the apk has problems.   This fixes the problem.  It's just intended to
 * to a quick install and then watch the log as it updates.
 *
 * It is missing the launcher info in the manifestfile on purpose.
 *
 */
public class MainActivity extends Activity {

    final static String TAG = "updater";
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.text);
        UpdateManager mUpdateManager = UpdateManager.getInstance();
        mUpdateManager.addStatusListener(new StatusListener() {
            @Override
            public void onStatusUpdate(UpdateManagerStatus status) {
                switch (status.currentState) {
                    case UpdateManagerStatus.STATE_UPDATE_AVAILABLE:
                        /* Notify user of the update */

                       // tv.setText("There is an update");
                        Log.wtf(TAG, "There is an update");
                        break;
                    case UpdateManagerStatus.STATE_DOWNLOADING_UPDATE:

                       // tv.setText("updating now.");
                        Log.wtf(TAG, "Updating now.");
                        /* Update UI to show progress */
                        break;
                    case UpdateManagerStatus.STATE_FINALIZING_UPDATE:
                        Log.wtf(TAG, "Finalizing now..");
                        break;
                    case UpdateManagerStatus.STATE_REPORTING_ERROR:
                        Log.wtf(TAG, "update errored!");
                        break;
                    case UpdateManagerStatus.STATE_UPDATED_NEEDS_REBOOT:
                        Log.wtf(TAG, "update done, needs a reboot");
                }
            }
        });

        // Trigger an update check immediately
        mUpdateManager.performUpdateNow(UpdatePolicy.POLICY_APPLY_AND_REBOOT);

    }
}
