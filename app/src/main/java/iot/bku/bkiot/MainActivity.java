package iot.bku.bkiot;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener, TextToSpeech.OnInitListener {


    final String TAG = "TEST_IOT";

    MQTTHelper mqttHelper;

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;



    TextView txtLocation;
    EditText[] txtIDs = new EditText[10];
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //startMQTT();
        txtLocation = findViewById(R.id.txtLocation);
        openUART();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        txtIDs[0] = findViewById(R.id.txtID_1);
        txtIDs[1] = findViewById(R.id.txtID_2);
        txtIDs[2] = findViewById(R.id.txtID_3);
        txtIDs[3] = findViewById(R.id.txtID_4);
        txtIDs[4] = findViewById(R.id.txtID_5);

        txtIDs[5] = findViewById(R.id.txtID_6);
        txtIDs[6] = findViewById(R.id.txtID_7);
        txtIDs[7] = findViewById(R.id.txtID_8);
        txtIDs[8] = findViewById(R.id.txtID_9);
        txtIDs[9] = findViewById(R.id.txtID_10);

        loadSettingData();

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettingData();
            }
        });

        //create an Intent
        Intent checkData = new Intent();
        //set it up to check for tts data
        checkData.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //start it so that it returns the result
        startActivityForResult(checkData, DATA_CHECKING);


        Timer aTimer = new Timer();
        TimerTask aTask = new TimerTask() {
            @Override
            public void run() {
                timerRun();
                ai_fsm();
            }
        };
        aTimer.schedule(aTask, 1000,1000);


    }
    int timer_counter = 0;
    int timer_flag = 0;
    public void timerRun(){
        if(timer_counter > 0){
            timer_counter --;
            if(timer_counter == 0){
                timer_flag = 1;
            }
        }
    }
    public void setTimer(int duration){
        timer_counter = duration;
        timer_flag = 0;
    }
    int status = 0;
    private void ai_fsm(){
        switch (status){
            case 0:
                break;
            case 1:
                //Talking
                talkToMe(ai_voice);
                setTimer(3);
                status = 2;
                break;
            case 2:
                if(timer_flag == 1){
                    status = 0;
                    txtLocation.setText("");
                }
                break;
            default:
                break;
        }
    }

    private void startMQTT() {
        mqttHelper = new MQTTHelper(getApplicationContext(), Ultis.hashCode(Ultis.getCPUSerial()));
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

                Log.d(TAG, "Init succesfull");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void openUART() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");
            txtLocation.setText("UART is note available");

        } else {
            Log.d("UART", "UART is available");
            txtLocation.setText("UART is available");

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
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    //port.write("ABC#".getBytes(), 1000);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);



                    Log.d("UART", "UART is openned");
                    txtLocation.setText("UART is openned");

                } catch (Exception e) {
                    Log.d("UART", "There is error");
                    txtLocation.setText("There is error");
                }
            }
        }

    }

    String buffer = "";
    String ai_voice = "";

    void processData(String data) {
        data = data.replaceAll("!", "");
        data = data.replaceAll("#", "");

        if(data.contains("1")){
            talkToMe("Xin chào anh A");
        }else if(data.contains("2")){
            talkToMe("Xin chào anh B");
        }else if(data.contains("3")){
            talkToMe("Xin chào anh C");
        }else if(data.contains("4")){
            talkToMe("Xin chào anh D");
        }



//        int index = Integer.parseInt(data);
//        txtLocation.setText("SA: " + data + "***" + index);
////        txtLocation.setText(index);
//        if(index <= 10){
//            ai_voice = txtIDs[index -1].getText().toString();
////            if(status == 0)
////                status = 1;
//            talkToMe(ai_voice);
//        }
    }

    @Override
    public void onNewData(byte[] data) {
        buffer += new String(data);
        Log.d("UART", "Received: " + new String(data));

        if (buffer.contains("!") && buffer.contains("#")) {
            processData(buffer);
            //txtLocation.setText("MA:" + buffer);
            buffer = "";
        }else{
            txtLocation.setText("NA:" + buffer);
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    public void saveSettingData(){
        for(int i = 0; i< 10; i++){
            String data = txtIDs[i].getText().toString();
            saveKey(this, "ID" + i, data);
        }
    }
    private void loadSettingData(){
        for(int i = 0; i < 10; i++){
            String data = loadKey(this, "ID" + i);
            txtIDs[i].setText(data);
        }
    }
    public void saveKey(Activity activity, String key, String value) {
        if (key.isEmpty()) return;
        SharedPreferences settings = activity.getSharedPreferences("Mobile_UART", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String loadKey(Activity activity, String key) {
        SharedPreferences settings = activity.getSharedPreferences("Mobile_UART", Context.MODE_PRIVATE);
        return settings.getString(key, "NA");
    }


    private int DATA_CHECKING = 0;
    private TextToSpeech niceTTS;
    @Override
    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            niceTTS.setLanguage(Locale.forLanguageTag("VI"));
            talkToMe("Xin chào các bạn, tôi là hệ thống trợ lý ảo nhân tạo. Bạn có thể hỏi tôi các vấn đề về nước");
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //do they have the data
        if (requestCode == DATA_CHECKING) {
            //yep - go ahead and instantiate
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
                niceTTS = new TextToSpeech(this, this);
                //no data, prompt to install it
            else {
                Intent promptInstall = new Intent();
                promptInstall.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(promptInstall);
            }
        }
    }

    public void talkToMe(String sentence) {
        String speakWords = sentence;
        niceTTS.speak(speakWords, TextToSpeech.QUEUE_FLUSH, null);
    }
}