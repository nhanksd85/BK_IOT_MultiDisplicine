package iot.bku.bkiot;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.suke.widget.SwitchButton;

public class SmartPower extends AppCompatActivity implements SwitchButton.OnCheckedChangeListener {

    SwitchButton labeledSwitch[] = new  SwitchButton[10];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_power);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        labeledSwitch[0] = findViewById(R.id.btnPower1);
        labeledSwitch[0].setOnCheckedChangeListener(this);

        labeledSwitch[1] = findViewById(R.id.btnPower2);
        labeledSwitch[1].setOnCheckedChangeListener(this);

        labeledSwitch[2] = findViewById(R.id.btnPower3);
        labeledSwitch[2].setOnCheckedChangeListener(this);

        labeledSwitch[3] = findViewById(R.id.btnPower4);
        labeledSwitch[3].setOnCheckedChangeListener(this);

        labeledSwitch[4] = findViewById(R.id.btnPower5);
        labeledSwitch[4].setOnCheckedChangeListener(this);

        labeledSwitch[5] = findViewById(R.id.btnPower6);
        labeledSwitch[5].setOnCheckedChangeListener(this);

        labeledSwitch[6] = findViewById(R.id.btnPower7);
        labeledSwitch[6].setOnCheckedChangeListener(this);

        labeledSwitch[7] = findViewById(R.id.btnPower8);
        labeledSwitch[7].setOnCheckedChangeListener(this);

        labeledSwitch[8] = findViewById(R.id.btnPower9);
        labeledSwitch[8].setOnCheckedChangeListener(this);

        labeledSwitch[9] = findViewById(R.id.btnPower10);
        labeledSwitch[9].setOnCheckedChangeListener(this);

    }


    @Override
    public void onCheckedChanged(SwitchButton view, boolean isChecked) {
        switch (view.getId()){
            case R.id.btnPower1:
                break;
            case R.id.btnPower2:
                break;
            case R.id.btnPower3:
                break;
            case R.id.btnPower4:
                break;
            case R.id.btnPower5:
                break;
            case R.id.btnPower6:
                break;
            case R.id.btnPower7:
                break;
            case R.id.btnPower8:
                break;
            case R.id.btnPower9:
                break;
            case R.id.btnPower10:
                break;
            default:
                break;
        }
    }
}
