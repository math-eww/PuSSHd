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
        if (server_info.setTheme) {
            this.setTheme(R.style.AppThemeDark);
        } else {
            this.setTheme(R.style.AppThemeLight);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Switch suSwitch = (Switch) findViewById(R.id.sutoggle);
        TextView suLabel = (TextView) findViewById(R.id.sutextoption);

        suLabel.setText("Use Root");
        suSwitch.setChecked(server_info.suEnabled);

        Switch themeSwitch = (Switch) findViewById(R.id.themetoggle);
        TextView themeLabel = (TextView) findViewById(R.id.themetextoption);

        themeLabel.setText("Use Dark Theme");
        themeSwitch.setChecked(server_info.setTheme);


        suSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    //Turn on SU
                    System.out.println("Enabling SU");
                    server_info.suEnabled = true;
                    writeSettingsFile(String.valueOf(server_info.suEnabled)+","+String.valueOf(server_info.setTheme));
                } else {
                    //Turn off SU
                    System.out.println("Disabling SU");
                    server_info.suEnabled = false;
                    writeSettingsFile(String.valueOf(server_info.suEnabled)+","+String.valueOf(server_info.setTheme));
                }
            }
        } );

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    //Turn on Dark Theme
                    System.out.println("Switching to Dark Theme");
                    server_info.setTheme = true;
                    writeSettingsFile(String.valueOf(server_info.suEnabled)+","+String.valueOf(server_info.setTheme));
                    server_info.scheduledRestart = true;
                } else {
                    //Turn on Light Theme
                    System.out.println("Switching to Light Theme");
                    server_info.setTheme = false;
                    writeSettingsFile(String.valueOf(server_info.suEnabled)+","+String.valueOf(server_info.setTheme));
                    server_info.scheduledRestart = true;
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
