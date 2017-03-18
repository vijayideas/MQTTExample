package com.fourway.mqttexample;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    MQTT mqtt = null;
    private final String TAG = "MQTTClient";
    private final String sAddress = "tcp://ec2-52-53-110-212.us-west-1.compute.amazonaws.com:61616";
//        private final String sAddress = "tcp://192.172.3.23:2883";
    //"http://ec2-52-53-110-212.us-west-1.compute.amazonaws.com:8161"; useraname:admin,pass:admin

    private ProgressDialog progressDialog = null;
    FutureConnection connection = null;

    EditText sendEditText,recEditText;
    Button sendButton,connectButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendEditText = (EditText)findViewById(R.id.msgSend);
        recEditText = (EditText)findViewById(R.id.msgRec);
        sendButton = (Button) findViewById(R.id.btnSnd);
        sendButton.setEnabled(false);
        connectButton = (Button) findViewById(R.id.connect);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = sendEditText.getText().toString();
                send(msg);
            }
        });

        connect();
    }


    // callback used for Future
    <T> Callback<T> onui(final Callback<T> original) {
        return new Callback<T>() {
            public void onSuccess(final T value) {
                runOnUiThread(new Runnable(){
                    public void run() {
                        original.onSuccess(value);
                    }
                });
            }
            public void onFailure(final Throwable error) {
                runOnUiThread(new Runnable(){
                    public void run() {
                        original.onFailure(error);
                    }
                });
            }
        };
    }

    private void connect(){
        mqtt = new MQTT();
        mqtt.setClientId("vijay-client-1");
        try
        {
            mqtt.setHost(sAddress);
            Log.d(TAG, "Address set: " + sAddress);
        }
        catch(URISyntaxException urise)
        {
            Log.e(TAG, "URISyntaxException connecting to " + sAddress + " - " + urise);
        }

        mqtt.setUserName("admin");
        mqtt.setPassword("admin");

        connection = mqtt.futureConnection();
        progressDialog = ProgressDialog.show(this, "",
                "Connecting...", true);
        connection.connect().then(onui(new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                progressDialog.dismiss();
                connectButton.setEnabled(false);
                sendButton.setEnabled(true);
                toast("Connected");
                subscribe();//subscribed
            }

            @Override
            public void onFailure(Throwable value) {
                toast("Problem connecting to host");
                Log.e(TAG, "Exception connecting to " + sAddress + " - " + value);
                progressDialog.dismiss();
                connectButton.setEnabled(true);
                sendButton.setEnabled(false);
            }
        }));
    }

    private void subscribe() {
        Topic[] topics = {new Topic("vijay", QoS.AT_LEAST_ONCE)};
        connection.subscribe(topics).then(onui(new Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] value) {
                connection.receive().then(onui(new Callback<Message>() {
                    @Override
                    public void onSuccess(Message message) {
                        String receivedMesageTopic = message.getTopic();
                        byte[] payload = message.getPayload();
                        String messagePayLoad = new String(payload);
                        message.ack();
                        recEditText.setText(receivedMesageTopic+": "+messagePayLoad);
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        Log.e(TAG, "Exception receiving message: " + value);
                    }
                }));
            }

            @Override
            public void onFailure(Throwable value) {
                Log.e(TAG, "Exception subscribe: " + value);
            }
        }));
    }

    private void send(String msg){
        connection.publish("vijay", msg.getBytes(), QoS.AT_LEAST_ONCE, false);
        sendEditText.setText("");
        toast("Message send");
    }

    private void toast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
