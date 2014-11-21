package info.mattsaunders.apps.pusshd;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Settings menu
 */
public class Settings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Switch suSwitch = (Switch) findViewById(R.id.sutoggle);
        TextView suLabel = (TextView) findViewById(R.id.sutextoption);

        suLabel.setText("Use Root");
        suSwitch.setChecked(server_info.suEnabled);

        suSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    //Turn on SU
                    System.out.println("Enabling SU");
                    server_info.suEnabled = true;
                } else {
                    //Turn off SU
                    System.out.println("Disabling SU");
                    server_info.suEnabled = false;
                }
            }
        } );
    }
}
