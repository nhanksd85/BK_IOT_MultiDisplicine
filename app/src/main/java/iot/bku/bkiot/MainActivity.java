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
import android.icu.util.LocaleData;
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
import java.util.regex.Pattern;

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
    int statusPump, statusLed;
    TextView txtPH, txtTDS, txtTemp, txtHumi;
    float currentTemp = 0;
    float currentHumidity = 0;
    float currentPh = 0;
    float currentTDS = 0;
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
        btnPump.setChecked(true);
        btnLed.setChecked(true);
        statusPump = - 1;
        statusLed = -1;
        txtPH = findViewById(R.id.txtPH);
        txtTDS = findViewById(R.id.txtTDS);
        txtTemp = findViewById(R.id.txtTemp);
        txtHumi = findViewById(R.id.txtHumi);
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
                if(topic.equals("nongnghiep40/feeds/V11")){
                    if(mqttMessage.toString().equals("1")){
                        Log.d("mqtt", "SET TRUE");
                        if(statusPump != 1) {
                            btnPump.setChecked(true);
                            Log.d("mqtt", "REAL SET TRUE");
                            statusPump = 1;
                        }

                    }
                    else {
                        Log.d("mqtt", "SET FALSE");

                        if(statusPump != 0) {
                            btnPump.setChecked(false);
                            Log.d("mqtt", "REAL SET TRUE");
                            statusPump = 0;
                        }
                    }
                }else if(topic.equals("nongnghiep40/feeds/V13")){
                    if(mqttMessage.toString().equals("1")){
                        if(statusLed != 1) {
                            btnLed.setChecked(true);
                            statusLed = 1;
                        }
                    }
                    else {
                        if(statusLed != 0) {
                            btnLed.setChecked(false);
                            statusLed = 0;
                        }
                    }
                }else if(topic.equals("nongnghiep40/feeds/V1")){
                    try {
                        currentPh = Float.parseFloat(mqttMessage.toString());
                        txtPH.setText(mqttMessage.toString());
                    }catch (Exception e){}
                }else if(topic.equals("nongnghiep40/feeds/V2")){
                    try {
                        currentTDS = Float.parseFloat(mqttMessage.toString());
                        txtTDS.setText(mqttMessage.toString() + "ppm");
                    }catch (Exception e){}
                }else if(topic.equals("nongnghiep40/feeds/V17")){
                    try {
                        currentTemp = Float.parseFloat(mqttMessage.toString());
                        txtTemp.setText(mqttMessage.toString() + "°C");
                    }catch (Exception e){}
                }else if(topic.equals("nongnghiep40/feeds/V16")){
                    try {
                        currentHumidity = Float.parseFloat(mqttMessage.toString());
                        txtHumi.setText(mqttMessage.toString() + "%");
                    }catch (Exception e){}
                }else if (topic.equals("nongnghiep40/feeds/V8")){

                    talkToMe(mqttMessage.toString());
                }else if (topic.equals("nongnghiep40/feeds/V9")){

                    processV9(mqttMessage.toString());
                }
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

    //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
    //ft:gpt-3.5-turbo-0613:personal::8IZFr4jR
    //http://lpnserver.net:51087/test?key=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&model=ft:gpt-3.5-turbo-0613:personal::8IZFr4jR&c=

    
    private void getChatGPTAnswer(String question){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtChatGPT.setText("Đang xử lý...");
                Log.d("mqtt", "Đang xử lý...");
            }
        });
        final Request request = new Request.Builder()
                .url("http://lpnserver.net:51087/test?c=" + question)
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
                    Log.d("mqtt", msg);
                    talkToMe(msg);
                }
            });
        }catch (Exception e){}
    }

    private void processV9(String msg){
        if(msg.contains("nhiệt độ") && msg.contains("vườn")){
            if(currentTemp > 0){
                talkToMe("Nhiệt độ của hệ thống hiện tại là " + currentTemp);
                try {
                    Thread.sleep(3000);
                    getChatGPTAnswer("Nhiệt độ là " + currentTemp + " có tốt cho cây trồng thủy canh?");
                }catch (Exception e){}
            }else{
                talkToMe("Thông tin nhiệt độ chưa được cập nhật");
            }
        }else if(msg.contains("độ ẩm") && msg.contains("vườn")){
            if(currentHumidity > 0){
                talkToMe("Độ ẩm của hệ thống hiện tại là " + currentHumidity);
                try {
                    Thread.sleep(3000);
                    getChatGPTAnswer("Độ ẩm là " + currentHumidity + " có tốt cho cây trồng thủy canh?");
                }catch (Exception e){}
            }else{
                talkToMe("Thông tin độ ẩm chưa được cập nhật");
            }
        }else if(msg.contains("ph") && msg.contains("vườn")){
            if(currentPh > 0){
                talkToMe("Nồng độ ph của hệ thống hiện tại là " + currentPh);
                try {
                    Thread.sleep(3000);
                    getChatGPTAnswer("Nồng độ ph là " + currentPh + " có tốt cho cây trồng thủy canh?");
                }catch (Exception e){}
            }else{
                talkToMe("Thông tin ph chưa được cập nhật");
            }
        }else if(msg.contains("tds") && msg.contains("vườn")){
            if(currentTDS > 0){
                talkToMe("Nồng độ tds của hệ thống hiện tại là " + currentTDS);
                try {
                    Thread.sleep(3000);
                    getChatGPTAnswer("Nồng độ tds là " + currentTDS + " có tốt cho cây trồng thủy canh?");
                }catch (Exception e){}
            }else{
                talkToMe("Thông tin tds chưa được cập nhật");
            }
        }else if(msg.contains("tình trạng hiện tại") && (msg.contains("vườn") || msg.contains("hệ thống"))){
            String ans = "";
            if(currentTemp > 0){
                ans += "Nhiệt độ của hệ thống hiện tại là " + currentTemp;

//                talkToMe("Nhiệt độ của hệ thống hiện tại là " + currentTemp);
//                try {
//                    Thread.sleep(5000);
//
//                }catch (Exception e){}
            }

            if(currentHumidity > 0){
                ans  += "  Độ ẩm là " + currentHumidity;

//                talkToMe("Độ ẩm là " + currentHumidity);
//                try {
//                    Thread.sleep(3000);
//
//                }catch (Exception e){}
            }
            if(currentPh > 0){
                ans += "  Nồng độ ph là " + currentPh;
//                talkToMe("Nồng độ ph là " + currentPh);
//                try {
//                    Thread.sleep(6000);
//
//                }catch (Exception e){}
            }
            if(currentTDS > 0){
                ans += "   Nồng độ tds của hệ thống hiện tại là " + currentTDS;

//                talkToMe("Nồng độ tds của hệ thống hiện tại là " + currentTDS);
//                try {
//                    Thread.sleep(4000);
//
//                }catch (Exception e){}
            }
            if (ans.length() > 0){
                talkToMe(ans.replaceAll(Pattern.quote("."), ","));
            }else {
                talkToMe("Hệ thống chưa được cập nhật");
            }

        }
        else {
            getChatGPTAnswer(msg);
        }
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
            Log.d("mqtt", requestCode + "****" +resultCode );
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d("mqtt", result.get(0));


                if (result.size() > 0) {
                    String msg = result.get(0).toLowerCase().trim();
                    processV9(msg);
                }
            }else{
                //isProcessingSearch = false;
                Log.d("mqtt", "You are here");
                if(data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("mqtt", result.get(0));
                }
            }
        }
    }



    public void talkToMe(final String sentence) {
        //sentence = sentence.replaceAll(Pattern.quote("."), ",");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtChatGPT.setText(sentence);
            }
        });

        String speakWords = sentence.replaceAll(Pattern.quote("."), ",");
        niceTTS.speak(speakWords, TextToSpeech.QUEUE_FLUSH, null);
    }
}