package edu.cs4730.robocarnearbycamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String ServiceId = "edu.cs4730.robocarnearbycamera";  //need a unique value to identify app.
    final static String TAG = "MainActivity";
    private String UserNickName = "RoboCar"; //idk what this should be.  doc's don't say.
    ImageView myImageView;
    Boolean mIsDiscovering = false;

    Button btn_con;

    private  String ConnectedEndPointId;
    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.  this is 1 to many, so 1 advertise and many discovery.
     * NOTE: in tests, the discovery changed the wifi to a hotspot on most occasions.  on disconnect, it changed back.
     */
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    //public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myImageView = findViewById(R.id.imageView);

        btn_con = findViewById(R.id.btn_connect);
        btn_con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovering();
                btn_con.setActivated(false);
            }
        });
        findViewById(R.id.btn_b).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("B");
            }
        });
        findViewById(R.id.btn_f).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("F");
            }
        });
        findViewById(R.id.btn_l).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("L");
            }
        });
        findViewById(R.id.btn_r).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("R");
            }
        });
        findViewById(R.id.btn_s).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("S");
            }
        });


    }

    void sendMessage(String item) {
        if (ConnectedEndPointId.compareTo("") != 0) { //ie if we are connected
            //now send the message to the car.
            send(item);
        }
    }

    /**
     * Sets the device to discovery mode.  Once an endpoint is found, it will initiate a connection.
     */
    protected void startDiscovering() {

        DiscoveryOptions.Builder builder = new  DiscoveryOptions.Builder();
        Nearby.getConnectionsClient(MainActivity.this).
            startDiscovery(
                MainActivity.ServiceId,   //id for the service to be discovered.  ie, what are we looking for.

                new EndpointDiscoveryCallback() {  //callback when we discovery that endpoint.
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        //we found an end point.
                        logthis("We found an endpoint " + endpointId + " name is " + info.getEndpointName());
                        //now make a initiate a connection to it.
                        makeConnection(endpointId);
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        logthis("End point lost  " + endpointId);
                        btn_con.setActivated(true);
                    }
                },

                builder.setStrategy(STRATEGY).build() )  //options for discovery.
            .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        mIsDiscovering = true;
                        logthis("We have started discovery.");
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mIsDiscovering = false;
                        logthis("We failed to start discovery.");
                        e.printStackTrace();
                        btn_con.setActivated(true);
                    }
                });

    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        Nearby.getConnectionsClient(MainActivity.this).stopDiscovery();
        logthis("Discovery Stopped.");
    }


    //the connection callback, both discovery and advertise use the same callback.
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
        new ConnectionLifecycleCallback() {
            private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();

            @Override
            public void onConnectionInitiated(
                String endpointId, ConnectionInfo connectionInfo) {
                // Automatically accept the connection on both sides.
                // setups the callbacks to read data from the other connection.
                Nearby.getConnectionsClient(MainActivity.this).acceptConnection(endpointId, //mPayloadCallback);
                    new PayloadCallback() {
                        @Override
                        public void onPayloadReceived(String endpointId, Payload payload) {

                            if (payload.getType() == Payload.Type.BYTES) {
                                String stuff = new String(payload.asBytes());
                                logthis("Received data is " + stuff);

                            } else if (payload.getType() == Payload.Type.FILE) {
                                logthis("We got a file. ");
                                // Add this to our tracking map, so that we can retrieve the payload later.
                                incomingPayloads.put(payload.getId(), payload);
                            } else if (payload.getType() == Payload.Type.STREAM)
                                //payload.asStream().asInputStream()
                                logthis("We got a stream, not handled");
                        }

                        @Override
                        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                            //if stream or file, we need to know when the transfer has finished.  ignoring this right now.
                            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                                Payload payload = incomingPayloads.remove(payloadTransferUpdate.getPayloadId());
                                if (payload.getType() == Payload.Type.FILE) {
                                    File payloadFile = payload.asFile().asJavaFile();
                                    Bitmap mypic = BitmapFactory.decodeFile(payloadFile.getAbsolutePath());
                                    myImageView.setImageBitmap(mypic);
                                }

                            }
                        }
                    });
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        stopDiscovering();
                        ConnectedEndPointId = endpointId;
                        logthis("Status ok, sending Hi message");
                        // send("Hi from Discovery");
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        logthis("Status rejected.  :(");
                        // The connection was rejected by one or both sides.
                        btn_con.setActivated(true);
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        logthis("Status error.");
                        // The connection broke before it was able to be accepted.
                        btn_con.setActivated(true);
                        break;
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                logthis("Connection disconnected :" + endpointId);
                ConnectedEndPointId = "";
                btn_con.setActivated(true);
            }
        };


    /**
     * Simple helper function to initiate a connect to the end point
     * it uses the callback setup above this function.
     */

    public void makeConnection(String endpointId) {
        Nearby.getConnectionsClient(MainActivity.this)
            .requestConnection(
                UserNickName,   //human readable name for the local endpoint.  if null/empty, uses device name or model.
                endpointId,
                mConnectionLifecycleCallback)
            .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        logthis("Successfully requested a connection");
                        // We successfully requested a connection. Now both sides
                        // must accept before the connection is established.
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Nearby Connections failed to request the connection.
                        logthis("failed requested a connection");
                        e.printStackTrace();
                    }
                });

    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     */
    protected void send(String data) {

        //basic error checking
        if (ConnectedEndPointId.compareTo("") == 0)   //empty string, no connection
            return;

        Payload payload = Payload.fromBytes(data.getBytes());

        // sendPayload (List<String> endpointIds, Payload payload)  if more then one connection allowed.
        Nearby.getConnectionsClient(MainActivity.this).
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


    @Override
    public void onStop() {
        super.onStop();
        stopDiscovering();
    }


    /**
     * helper function to log and add to a textview.
     */
    public void logthis(String msg) {
        //logger.append(msg + "\n");
        Log.d(TAG, msg);
    }


}
