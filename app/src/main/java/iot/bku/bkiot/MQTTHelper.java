package iot.bku.bkiot;

/**
 * Created by USER on 5/15/2021.
 */

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Le Trong Nhan on 18/02/2020.
 */

public class MQTTHelper {


//    nongnghiep40/feeds/V1: pH
//    nongnghiep40/feeds/V2: tds
//    nongnghiep40/feeds/V11: máy bơm
//    nongnghiep40/feeds/V13: đèn
//    nongnghiep40/feeds/V16: độ ẩm
//    nongnghiep40/feeds/V17: nhiệt độ

    final String serverUri = "tcp://mqtt.ohstem.vn:1883";


    private String clientId = "";
    final String[] subscriptionTopic = {
            "nongnghiep40/feeds/V1",
            "nongnghiep40/feeds/V2",
            "nongnghiep40/feeds/V16",
            "nongnghiep40/feeds/V17",
            "nongnghiep40/feeds/V11",
            "nongnghiep40/feeds/V13"};


    final String username = "nongnghiep40";
    final String password = "aio_uGgA75WHzFBjnu72A2CQ7bUnBcpm";

    public MqttAndroidClient mqttAndroidClient;


    public MQTTHelper(Context context, String _clientID){
        clientId = _clientID;
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    private void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void subscribeToTopic() {
        for (int i = 0; i < subscriptionTopic.length; i++) {
            try {
                mqttAndroidClient.subscribe(subscriptionTopic[i], 0, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.w("Mqtt", "Subscribed!");

                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.w("Mqtt", "Subscribed fail!");
                        }
                    });

            } catch(MqttException ex){
                System.err.println("Exceptionst subscribing");
                ex.printStackTrace();
            }

        }
    }
}
