package info.mattsaunders.apps.pusshd;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Settings menu
 */
public class Settings extends Activity {

    private static final String FILENAME = "settings";

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
                    writeSettingsFile(String.valueOf(server_info.suEnabled));
                } else {
                    //Turn off SU
                    System.out.println("Disabling SU");
                    server_info.suEnabled = false;
                    writeSettingsFile(String.valueOf(server_info.suEnabled));
                }
            }
        } );
    }

    public static void writeSettingsFile(String data) {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/PuSSHd/");
            dir.mkdir();
            File file = new File(dir, FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data.getBytes());
                //Log.i("Successfully wrote JSON to file", data.toString());
            } catch (Exception ex) {
                Log.e("Failed to save pid", data);
                ex.printStackTrace();
            }
            fos.close();
        } catch (Exception ex) {
            Log.e("Failed to open file", FILENAME);
            ex.printStackTrace();
        }
    }
}
