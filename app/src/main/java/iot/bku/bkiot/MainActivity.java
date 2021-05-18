package iot.bku.bkiot;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    public class Constants {
        public static final int NUM_TEXTVIEWS = 17;
    }

    MQTTHelper mqttHelper;
    final String TAG = "TEST_IOT";
    private String buffer = "";
    TextView[] textViews = new TextView[Constants.NUM_TEXTVIEWS];

    JSONObject jsonObjectSend = new JSONObject();
    JSONArray jsonArray;

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    UsbSerialPort port;

    private void sendDataMQTT(String data){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);

        byte[] b = data.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        Log.d("ABC","Publish :" + msg);
        try {
            mqttHelper.mqttAndroidClient.publish("NPNLab_BBC/f/+", msg);
        } catch (MqttException e){

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < Constants.NUM_TEXTVIEWS; i++) {
            String buttonID = "inout" + i;
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            textViews[i] = findViewById(resID);
        }

        String string = "nothing";
        InputStream inputStream = getResources().openRawResource(R.raw.info);
        try {
            byte[] buffer = new byte[inputStream.available()];
            while (inputStream.read(buffer) != -1) {
                string = new String(buffer);
            }
        } catch (IOException e) {
            Log.d("exception", e.toString());
        }
        try {
            jsonArray = new JSONArray(string);
        } catch (JSONException e) {
            Log.d("jsonarray", "can not convert");
        }
        openUART();
        startMQTT();
    }

    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), Ultis.hashCode(Ultis.getCPUSerial()));
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(TAG, "Init successful");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, "Message arrived");
                String dataToGateway = "";
                try {
                    JSONObject jsonObjectReceive = new JSONObject(mqttMessage.toString());
                    String ID = jsonObjectReceive.getString("ID");
                    String value = jsonObjectReceive.getString("Value");
                    int idInt = Integer.parseInt(ID);
                    switch (idInt) {
                        case 1:
                            textViews[10].setText(value);
                            break;
                        case 2:
                            textViews[11].setText(value);
                            break;
                        case 3:
                            textViews[12].setText(value);
                            break;
                        case 4:
                            textViews[0].setText(value);
                            break;
                        case 5:
                            textViews[1].setText(value);
                            break;
                        case 6:
                            textViews[13].setText(value);
                            break;
                        case 7:
                            textViews[2].setText(value);
                            break;
                        case 8:
                            textViews[3].setText(value);
                            break;
                        case 9:
                            textViews[4].setText(value);
                            break;
                        case 10:
                            textViews[14].setText(value);
                            break;
                        case 11:
                            textViews[15].setText(value);
                            break;
                        case 12:
                            textViews[5].setText(value);
                            break;
                        case 13:
                            textViews[6].setText(value);
                            break;
                        case 16:
                            textViews[7].setText(value);
                            break;
                        case 17:
                            textViews[8].setText(value);
                            break;
                        case 22:
                            textViews[9].setText(value);
                            break;
                        case 23:
                            textViews[16].setText(value);
                            break;
                    }
                    dataToGateway = "!" + ID + ":" + value + "#";
                } catch (JSONException e) {
                    Log.e("JSONException", "Error: " + e.toString());
                }
                try {
                    port.write(dataToGateway.getBytes(), 1000);
                } catch (Exception e) {

                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
    private void openUART(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");

        }else {
            Log.d("UART", "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));


                return;
            } else {

                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    //port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    //port.write("ABC#".getBytes(), 1000);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                    Log.d("UART", "UART is openned");

                } catch (Exception e) {
                    Log.d("UART", "There is error");
                }
            }
        }

    }

    @Override
    public void onNewData(byte[] data) {
        buffer += new String(data);
        Log.d("UART", "Received: " + new String(data));
        if (buffer.contains("!") && buffer.contains("#")) {
            try {
                int index_soc = buffer.indexOf("!");
                int index_eoc = buffer.indexOf("#");
                if (index_soc < index_eoc) {
                    String sentData = buffer.substring(index_soc + 1, index_eoc);
                    int colonIndex = sentData.indexOf(":");
                    String ID = sentData.substring(0, colonIndex);
                    String Value = sentData.substring(colonIndex + 1, sentData.length());
                    JSONObject jsonObjectData = jsonArray.getJSONObject(Integer.parseInt(ID) - 1);
                    String name = jsonObjectData.getString("name");
                    String unit = jsonObjectData.getString("unit");
                    jsonObjectSend.put("ID", ID);
                    jsonObjectSend.put("Name", name);
                    jsonObjectSend.put("Value", Value);
                    jsonObjectSend.put("Unit", unit);
                    sendDataMQTT(jsonObjectSend.toString());
                    buffer = "";
                }
            } catch (Exception e) {
                sendDataMQTT("Fault");
                buffer = "";
            }
        }
    }

    @Override
    public void onRunError(Exception e) {

    }
}
