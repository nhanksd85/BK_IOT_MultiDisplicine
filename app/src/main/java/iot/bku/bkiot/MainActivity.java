package iot.bku.bkiot;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.suke.widget.SwitchButton;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener, TextToSpeech.OnInitListener {


    OkHttpClient client = new OkHttpClient();

    final String TAG = "TEST_IOT";

    MQTTHelper mqttHelper;

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;



    TextView txtChatGPT;
    EditText[] txtIDs = new EditText[10];
    Button btnSave;
    private ImageButton btnVoice;
    SwitchButton btnPump, btnLed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startMQTT();

        //openUART();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);






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



        btnVoice = findViewById(R.id.btnVoice);

        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceInput();
            }
        });


        txtChatGPT = findViewById(R.id.txtChatGPT);
        txtChatGPT.setMovementMethod(new ScrollingMovementMethod());

        btnPump = findViewById(R.id.btnPump);
        btnLed = findViewById(R.id.btnLed);

        btnPump.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked){
                    sendDataMQTT("nongnghiep40/feeds/V11", "1");
                }else{
                    sendDataMQTT("nongnghiep40/feeds/V11", "0");
                }
            }
        });
        btnLed.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked){
                    sendDataMQTT("nongnghiep40/feeds/V13", "1");
                }else{
                    sendDataMQTT("nongnghiep40/feeds/V13", "0");
                }
            }
        });
    }


    public void sendDataMQTT(String topic, String value){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(false);

        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (MqttException e){
        }
    }

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    public void startVoiceInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,"vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Xin mời nói...");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        try {
            //isProcessingSearch = true;
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            //layoutHeader.setVisibility(View.INVISIBLE);
        } catch (ActivityNotFoundException a) {
            //isProcessingSearch = false;
            //layoutHeader.setVisibility(View.VISIBLE);
        }

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
                    //txtLocation.setText("");
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

                Log.d("mqtt", topic + "*****" + mqttMessage.toString());
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
            //txtLocation.setText("UART is note available");

        } else {
            Log.d("UART", "UART is available");
            //txtLocation.setText("UART is available");

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
                    //txtLocation.setText("UART is openned");

                } catch (Exception e) {
                    Log.d("UART", "There is error");
                    //txtLocation.setText("There is error");
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
            //txtLocation.setText("NA:" + buffer);
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
            //talkToMe("Xin chào các bạn, tôi là hệ thống trợ lý ảo nhân tạo. Bạn có thể hỏi tôi các vấn đề về nước");
        }
    }

    private void getChatGPTAnswer(String question){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtChatGPT.setText("Đang xử lý...");
            }
        });
        final Request request = new Request.Builder()
                .url("http://lpnserver.net:51087/chat?c=" + question)
                .build();
        try {
            //Response response = client.newCall(request).execute();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {

                }

                @Override
                public void onResponse(Response response) throws IOException {
                    String msg = response.body().string();

                    talkToMe(msg);
                }
            });
        }catch (Exception e){}
    }


    @Override
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
        }else if(requestCode == REQ_CODE_SPEECH_INPUT){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d("Assistant", result.get(0));


                if (result.size() > 0) {
                    String msg = result.get(0).toLowerCase().trim();
                    getChatGPTAnswer(msg);
                }
            }else{
                //isProcessingSearch = false;
            }
        }
    }



    public void talkToMe(final String sentence) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtChatGPT.setText(sentence);
            }
        });

        String speakWords = sentence;
        niceTTS.speak(speakWords, TextToSpeech.QUEUE_FLUSH, null);
    }
}