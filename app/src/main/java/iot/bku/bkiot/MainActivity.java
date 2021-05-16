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

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    MQTTHelper mqttHelper;
    final String TAG = "TEST_IOT";

    JSONObject jsonObjectSend = new JSONObject();

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

        openUART();
        startMQTT();
    }

    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), Ultis.hashCode(Ultis.getCPUSerial()));
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(TAG, "Init succesfull");
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
                    String data = jsonObjectReceive.getString("Data");
                    dataToGateway = "!" + ID + ":" + data + "#";
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

    private String buffer = "";
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
                    jsonObjectSend.put("ID", sentData.substring(0, colonIndex));
                    jsonObjectSend.put("Data", sentData.substring(colonIndex + 1, sentData.length()));
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
