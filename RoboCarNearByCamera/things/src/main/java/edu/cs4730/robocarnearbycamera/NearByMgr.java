package edu.cs4730.robocarnearbycamera;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileNotFoundException;


public class NearByMgr {
   private static final String ServiceId = "edu.cs4730.robocarnearbycamera";  //need a unique value to identify app.
    final static String TAG = "NearByMgr";
    private String UserNickName = "RoboCar"; //idk what this should be.  doc's don't say.

    private  String ConnectedEndPointId;
    private Context context;
    private boolean mIsAdvertising = false;
    private TextView logger = null;

    private OnNearByCallback mCallback;


    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.  this is 1 to many, so 1 advertise and many discovery.
     * NOTE: in tests, the discovery changed the wifi to a hotspot on most occasions.  on disconnect, it changed back.
     */
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    //public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;


    NearByMgr(Context c) {
        context = c;
    }

    NearByMgr(Context c, OnNearByCallback callback) {
        context = c;
        mCallback = callback;
    }

    /**
     * Start advertising the nearby.  It sets the callback from above with what to once we get a connection
     * request.
     */
    public void startAdvertising() {

        AdvertisingOptions.Builder builder = new  AdvertisingOptions.Builder();

        Nearby.getConnectionsClient(context)
            .startAdvertising(
                UserNickName,    //human readable name for the endpoint.
                ServiceId,  //unique identifier for advertise endpoints
                mConnectionLifecycleCallback,  //callback notified when remote endpoints request a connection to this endpoint.
               // new AdvertisingOptions(STRATEGY))
                builder.setStrategy(STRATEGY).build())
            .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        mIsAdvertising = true;
                        logthis("we're advertising!");
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mIsAdvertising = false;
                        // We were unable to start advertising.
                        logthis("we're failed to advertise");
                        e.printStackTrace();
                    }
                });
    }

    /**
     * turn off advertising.  Note, you can not add success and failure listeners.
     */
    public void stopAdvertising() {
        mIsAdvertising = false;
        Nearby.getConnectionsClient(context).stopAdvertising();
        logthis("Advertising stopped.");
    }

    /**
     * Callbacks for connections to other devices.  These call backs are used when a connection is initiated
     * and connection, and disconnect.
     * we auto accept any connection.  We with another callback that allows us to read the data.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
        new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                logthis("Connection Initiated :" + endpointId + " Name is " + connectionInfo.getEndpointName());
                // Automatically accept the connection on both sides.
                // setups the callbacks to read data from the other connection.
                Nearby.getConnectionsClient(context).acceptConnection(endpointId, //mPayloadCallback);
                    new PayloadCallback() {
                        @Override
                        public void onPayloadReceived(String endpointId, Payload payload) {

                            if (payload.getType() == Payload.Type.BYTES) {
                                String stuff = new String(payload.asBytes());
                                logthis("Received data is " + stuff);
                                //ASDF here!
                                if (mCallback != null)  mCallback.onDataBytes(stuff);
                            } else if (payload.getType() == Payload.Type.FILE)
                                logthis("We got a file.  not handled");
                            else if (payload.getType() == Payload.Type.STREAM)
                                //payload.asStream().asInputStream()
                                logthis("We got a stream, not handled");
                        }

                        @Override
                        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                            //if stream or file, we need to know when the transfer has finished.  ignoring this right now.
                        }
                    });
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
                logthis("Connection accept :" + endpointId + " result is " + result.toString());

                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        ConnectedEndPointId = endpointId;
                        //if we don't then more can be added to conversation, when an List<string> of endpointIds to send to, instead a string.
                        // ... .add(endpointId);
                        stopAdvertising();  //and comment this out to allow more then one connection.
                        logthis("Connected, now launch the picture taking.  wish it was stream... ");

                        if (mCallback != null)  mCallback.onConnectionStatus(result.getStatus().getStatusCode());
                       //ASDF  do something here!

                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        logthis("Status rejected.  :(");
                        // The connection was rejected by one or both sides.
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        logthis("Status error.");
                        // The connection broke before it was able to be accepted.
                        break;
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                logthis("Connection disconnected :" + endpointId);
                ConnectedEndPointId = "";  //need a remove if using a list.
                mCallback.onConnectionStatus(-1);  //hope it's not in use.
            }
        };

    public interface OnNearByCallback {
        public void onConnectionStatus(int Status);
        public void onDataBytes(String Data);
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     */
    public void sendFile(File file) {

        //basic error checking
        if (ConnectedEndPointId.compareTo("") == 0)   //empty string, no connection
            return;

        Uri uri = Uri.fromFile(file);
        // Open the ParcelFileDescriptor for this URI with read access.
        ParcelFileDescriptor pfd = null;
        try {
            pfd = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Payload payload = Payload.fromFile(pfd);


        //Payload payload = Payload.fromBytes(data.getBytes());

        // sendPayload (List<String> endpointIds, Payload payload)  if more then one connection allowed.
        Nearby.getConnectionsClient(context).
            sendPayload(ConnectedEndPointId,  //end point to end to
                payload)   //the actual payload of data to send.
            .addOnSuccessListener(new OnSuccessListener<Void>() {  //don't know if need this one.
                @Override
                public void onSuccess(Void aVoid) {
                    logthis("Message send successfully.");
                }
            })
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    logthis("Message send completed.");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    logthis("Message send failed.");
                    e.printStackTrace();
                }
            });
    }

    private void logthis(String item) {
        if (logger != null)   logger.append(item + "\n");
        Log.d(TAG, item);
    }
}
