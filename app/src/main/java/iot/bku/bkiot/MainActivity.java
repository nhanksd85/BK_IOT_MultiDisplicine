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
import android.widget.Button;
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

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener, View.OnClickListener {

    public class Constants {
        public static final int NUM_INOUTS = 17;
        public static final int NUM_DEVICES = 23;
    }

    MQTTHelper mqttHelper;
    final String TAG = "TEST_IOT";
    private String buffer = "";
    TextView[] textViews = new TextView[Constants.NUM_INOUTS];
    TextView temp, humid;
    Button buttonExit;
    int scanFirstID;

    JSONObject jsonObjectSend = new JSONObject();
    JSONArray jsonArray;

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    UsbSerialPort port;

    private void sendDataMQTT(String data, String ID){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);

        byte[] b = data.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        Log.d("ABC","Publish :" + msg);
        try {
            switch (ID) {
                case "1":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-led", msg);
                    break;
                case "2":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-speaker", msg);
                    break;
                case "3":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-lcd", msg);
                    break;
                case "4":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-button", msg);
                    break;
                case "5":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-touch", msg);
                    break;
                case "6":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-traffic", msg);
                    break;
                case "7":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-temp-humid", msg);
                    break;
                case "8":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-magnetic", msg);
                    break;
                case "9":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-soil", msg);
                    break;
                case "10":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC/feeds/bk-iot-drv", msg);
                    break;
                case "11":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-relay", msg);
                    break;
                case "12":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-sound", msg);
                    break;
                case "13":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-light", msg);
                    break;
                case "16":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-infrared", msg);
                    break;
                case "17":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-servo", msg);
                    break;
                case "22":
                    mqttHelper.mqttAndroidClient1.publish("CSE_BBC1/feeds/bk-iot-time", msg);
                    break;
                case "23":
                    mqttHelper.mqttAndroidClient.publish("CSE_BBC1/feeds/bk-iot-gas", msg);
                    break;
            }
        } catch (MqttException e){

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < Constants.NUM_INOUTS; i++) {
            String buttonID = "inout" + i;
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            textViews[i] = findViewById(resID);
        }

        temp = findViewById(R.id.txtTemp);
        humid = findViewById(R.id.txtHumi);

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
        try {
            for (scanFirstID = 1; scanFirstID < Constants.NUM_DEVICES + 1; scanFirstID++) {
                JSONObject jsonObjectInit = jsonArray.getJSONObject(scanFirstID - 1);
                String apiURL = jsonObjectInit.getString("lastURL");
                boolean none = (apiURL.equals("none"));
                if (!apiURL.equals("none")) {
                    Log.d("apiURL", apiURL);
                    Log.d("length", Integer.toString(apiURL.length()));
                    Log.d("none true?", Boolean.toString(none));
                    //receiveLastDataFromServer(scanFirstID, apiURL);
                    try {
                        receiveLastDataFromServer(scanFirstID, apiURL);
                    }catch (Exception e){}
                }
            }
        } catch (JSONException e) {

        }

        openUART();
        startMQTT();
        buttonExit = findViewById(R.id.btnExit);
        buttonExit.setOnClickListener(this);
        setupWDT();
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
                Log.d(TAG, mqttMessage.toString());
                String dataToGateway = "";
                try {
                    JSONObject jsonObjectReceive = new JSONObject(mqttMessage.toString());
                    String ID = jsonObjectReceive.getString("ID");
                    String value = jsonObjectReceive.getString("Value");
                    updateTextView(ID, value);
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

    private final int MAX_COUNTER = 60;
    private int wdt_counter = MAX_COUNTER;
    private void setupWDT(){
        Timer wd_timer = new Timer();
        TimerTask wd_task = new TimerTask() {
            @Override
            public void run() {
                wdt_counter--;
                if(wdt_counter <=0){
                    wdt_counter = MAX_COUNTER;
                    try{
                        port.close();
                    }catch (Exception e){}
                    openUART();
                }
            }
        };
        wd_timer.schedule(wd_task,2000,1000);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnExit) {
            finish();
            System.exit(0);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        //reset wdt
        wdt_counter = MAX_COUNTER;

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
                    sendDataMQTT(jsonObjectSend.toString(), ID);
                    buffer = "";
                }
            } catch (Exception e) {
                sendDataMQTT("Fault", "0");
                buffer = "";
            }
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    private void receiveLastDataFromServer(final int ID, String apiURL){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(apiURL).build();
        try {
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    String initDataReceived = response.body().string();
                    try {
                        JSONObject jsonObjectInitReceive = new JSONObject(initDataReceived);
                        String receive = jsonObjectInitReceive.getString("value");
                        JSONObject jsonObjectInitValue = new JSONObject(receive);
                        String value = jsonObjectInitValue.getString("Value");
                        String id = Integer.toString(ID);
                        Log.d("id", id);
                        updateTextView(id, value);
                    } catch (JSONException e) {
                        Log.e("JSONException", "Error: " + e.toString());
                    }
                }
            });
        } catch (Exception e) {
            Log.d("Fail", "Get json");
        }
    }

    private void updateTextView(String ID, final String value) {
        final int idInt = Integer.parseInt(ID);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (idInt) {
                    case 1:
                        Log.d("1", value);
                        textViews[9].setText(value);
                        break;
                    case 2:
                        Log.d("2", value);
                        textViews[10].setText(value);
                        break;
                    case 3:
                        Log.d("3", value);
                        textViews[11].setText(value);
                        break;
                    case 4:
                        Log.d("4", value);
                        textViews[0].setText(value);
                        break;
                    case 5:
                        Log.d("5", value);
                        textViews[1].setText(value);
                        break;
                    case 6:
                        Log.d("6", value);
                        textViews[12].setText(value);
                        break;
                    case 7:
                        Log.d("7", value);
                        try {
                            temp.setText(value.substring(0, value.indexOf('-')) + "\u2103");
                            humid.setText(value.substring(value.indexOf('-') + 1, value.length()) + "\u0025");
                        } catch (Exception e) {

                        }
                        break;
                    case 8:
                        Log.d("8", value);
                        textViews[2].setText(value);
                        break;
                    case 9:
                        Log.d("9", value);
                        textViews[3].setText(value);
                        break;
                    case 10:
                        Log.d("10", value);
                        textViews[13].setText(value);
                        break;
                    case 11:
                        Log.d("11", value);
                        textViews[14].setText(value);
                        break;
                    case 12:
                        Log.d("12", value);
                        textViews[4].setText(value);
                        break;
                    case 13:
                        Log.d("13", value);
                        textViews[5].setText(value);
                        break;
                    case 16:
                        Log.d("16", value);
                        textViews[6].setText(value);
                        break;
                    case 17:
                        Log.d("17", value);
                        textViews[15].setText(value);
                        break;
                    case 22:
                        Log.d("22", value);
                        textViews[7].setText(value);
                        break;
                    case 23:
                        Log.d("23", value);
                        textViews[8].setText(value);
                        break;
                }
            }
        });

    }
}
