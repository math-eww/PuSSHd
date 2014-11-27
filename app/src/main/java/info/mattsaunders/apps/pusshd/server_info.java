package info.mattsaunders.apps.pusshd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.sshd.SshServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;


public class server_info extends Activity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private static Context context;
    private static final String FILENAME = "PuSSHd_settings";
    private static final String FILENAME_SETTINGS = "settings";

    public static SshServer sshd;
    public static int sshdPid;
    public static Logger log;
    public static boolean mRunning = false;
    public static Handler mHandler = new Handler();
    public static Runnable mUpdater;
    public static boolean statusOnOff;
    public static String[] userSettings = readSettingsFile().split(",");
    public static boolean suEnabled = Boolean.valueOf(userSettings[0]);
    public static boolean setTheme;
    public static boolean scheduledRestart = false;

    public static Context getAppContext(){
        return server_info.context;
    }
    public static boolean getAppTheme() {
        userSettings = readSettingsFile().split(",");
        if (userSettings.length > 1) {
            setTheme = Boolean.valueOf(userSettings[1]);
        } else {
            setTheme = false;
        }
        System.out.println("Use dark theme: " + setTheme);
        return setTheme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getAppTheme()) {
            this.setTheme(R.style.AppThemeDark);
        } else {
            this.setTheme(R.style.AppThemeLight);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        //Init context variable
        server_info.context = getApplicationContext();

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scheduledRestart)
        {
            scheduledRestart = false;
            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            System.out.println("Settings selected");
            startActivity(new Intent(this, Settings.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: //Server Controls
                    return new ServerControls();
                case 1: //Server Log
                    return new ServerLog();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
        /*
        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
        */
    }

    public static class ServerControls extends Fragment {

        public ServerControls() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_server_info, container, false);
            setupServerToggle(rootView);
            return rootView;
        }
    }

    public static class ServerLog extends Fragment {

        public ServerLog() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_server_log, container, false);
            setupServerLog(rootView);
            return rootView;
        }
    }

    public static void setupServerLog (View v) {
        //TODO: get more info from SSH object to display in this fragment screen
        final TextView status = (TextView) v.findViewById(R.id.status);
        status.setText("Server Stopped");
        final TextView version = (TextView) v.findViewById(R.id.version);
        version.setText("");
        final TextView activeSessionLabel = (TextView) v.findViewById(R.id.activeSessions);
        activeSessionLabel.setText("");
        final ListView list = (ListView) v.findViewById(R.id.userlist);

        mUpdater = new Runnable() {
            @Override
            public void run() {
                if (!mRunning) {
                    status.setText("Server Stopped");
                    version.setText("");
                    activeSessionLabel.setText("");
                    return;
                }
                status.setText("Server Running");
                if (sshd != null) {
                    version.setText(sshd.getVersion());
                    activeSessionLabel.setText("Active Sessions:");
                    ArrayList<Object> userList = new ArrayList<>(Arrays.asList(sshd.getActiveSessions().toArray()));
                    SessionListAdapter itemAdapter = new SessionListAdapter(getAppContext(), userList);
                    list.setAdapter(itemAdapter);
                }
                mHandler.postDelayed(this, 500); // set time here to refresh views
            }
        };

        //Get server's status on orientation change and set up accordingly:
        if (statusOnOff) {
            mRunning = true;
            mHandler.post(mUpdater);
        }

    }

    public static void setupServerToggle(View v) {
        //Set button label:
        final ToggleButton serverToggle = (ToggleButton)v.findViewById(R.id.serverToggle);
        serverToggle.setText("Start Server");

        if (statusOnOff) {
            //isChecked = true;
            serverToggle.setChecked(true);
            serverToggle.setText("Stop Server");
        }

        //Set text labels:
        final TextView username1 = (TextView) v.findViewById(R.id.username1);
        username1.setText("Username");
        final TextView password2 = (TextView) v.findViewById(R.id.password2);
        password2.setText("Password");
        final TextView port3 = (TextView) v.findViewById(R.id.port3);
        port3.setText("Port");

        //Get IP and set label:
        //final String ip = getLocalIpAddress();
        final String ip = getIpAddr();
        final TextView ipaddress = (TextView) v.findViewById(R.id.ipaddress);
        ipaddress.setText(ip);

        //Set connect info to empty string: (when server starts, it will be set to correct info)
        final TextView connectionInfo = (TextView) v.findViewById(R.id.connectinfo);
        connectionInfo.setText("");

        //Store fields for user input:
        final EditText inputUser = (EditText) v.findViewById(R.id.userenter);
        final EditText inputPass = (EditText) v.findViewById(R.id.passenter);
        final EditText inputPort = (EditText) v.findViewById(R.id.portenter);
        //Load saved user data if available, default if not
        JSONObject obj = readJsonFile();
        if (obj != null) {
            Bundle userdata = JsonObjectToBundle(obj);
            inputUser.setText(userdata.getString("USER"));
            inputPass.setText(userdata.getString("PASS"));
            inputPort.setText(userdata.getString("PORT"));
        } else {
            inputUser.setText("test");
            inputPass.setText("test");
            inputPort.setText("2000");
        }

        System.out.println("SU enabled: " + suEnabled);

        //Set button listener:
        serverToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    if (isChecked) {
                        //Get user's input:
                        String username = inputUser.getText().toString();
                        String password = inputPass.getText().toString();
                        String port = inputPort.getText().toString();
                        String ip = getIpAddr();

                        //Server starting: set labels correctly:
                        connectionInfo.setText(username + ":" + password + "@");
                        ipaddress.setText(ip + ":" + port);
                        //Write to log:
                        System.out.println("Server Starting: " + username + ":" + password + "@" + ip + ":" + port);

                        //Start the server:
                        Bundle extras = new Bundle();
                        extras.putString("IP", ip);
                        extras.putString("PORT", port);
                        extras.putString("USER", username);
                        extras.putString("PASS", password);
                        Intent i;
                        i = new Intent(getAppContext(), server_service.class);
                        i.putExtras(extras);
                        getAppContext().startService(i);

                        //Save JSON object with user info:
                        JSONObject json = bundleToJsonObject(extras); //Convert bundle to JSON
                        if (json != null) {
                            writeJsonFile(json);
                        } //Write JSON to file

                        //Begin updating log view:
                        mRunning = true;
                        mHandler.post(mUpdater);

                        //Save server state to variable
                        statusOnOff = true;

                    } else {
                        //Server stopping: set labels correctly:
                        connectionInfo.setText("");
                        ipaddress.setText(ip);
                        //Write to log:
                        System.out.println("Server Stopping...");

                        //Stop the server:
                        Intent i;
                        i = new Intent(getAppContext(), server_service.class);
                        getAppContext().stopService(i);
                        try {
                            sshd.stop();
                            Log.i("SUCCESS: Server stopped", sshd.toString());
                            Toast.makeText(getAppContext(), "SSH Service Stopped", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("FAILURE: Server failed to stop. Is it running?", e.toString());
                        }

                        mRunning = false;
                        sshd = null;
                        log = null;

                        //Save server state to variable
                        statusOnOff = false;
                    }
                }
            }
        });
        if (statusOnOff) {
            String username = inputUser.getText().toString();
            String password = inputPass.getText().toString();
            String port = inputPort.getText().toString();
            connectionInfo.setText(username + ":" + password + "@");
            ipaddress.setText(ip + ":" + port);
        }
    }

    public static String getIpAddr() {
        try {
            Context c = getAppContext();
            WifiManager wifiManager = (WifiManager) c.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
            return "000.000.0.0";
        }
    }

    public static JSONObject bundleToJsonObject(Bundle bundle) {
        try {
            JSONObject output = new JSONObject();
            for( String key : bundle.keySet() ){
                Object object = bundle.get(key);
                if(object instanceof Integer || object instanceof String)
                    output.put(key, object);
                else
                    throw new RuntimeException("only Integer and String can be extracted");
            }
            return output;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bundle JsonObjectToBundle(JSONObject jsonObject) {
        try {
            Bundle bundle = new Bundle();
            Iterator<?> keys = jsonObject.keys();
            while( keys.hasNext() ){
                String key = (String)keys.next();
                Object object = jsonObject.get(key);
                if(object instanceof String)
                    bundle.putString(key, (String) object);
                else if(object instanceof Integer)
                    bundle.putInt(key, (Integer) object);
                else
                    throw new RuntimeException("only Integer and String can be re-extracted");
            }
            return bundle;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJsonFile(JSONObject data) {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/PuSSHd/");
            dir.mkdir();
            File file = new File(dir, FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data.toString().getBytes());
            } catch (Exception ex) {
                Log.e("Failed to save data", data.toString());
                ex.printStackTrace();
            }
            fos.close();
        } catch (Exception ex) {
            Log.e("Failed to open file", FILENAME);
            ex.printStackTrace();
        }
    }

    public static JSONObject readJsonFile() {
        String json;
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/PuSSHd/");
            File file = new File(dir, FILENAME);
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader fileRead = new InputStreamReader(fis);
                BufferedReader reader = new BufferedReader(fileRead);
                String str;
                StringBuilder buf = new StringBuilder();
                try {
                    while ((str = reader.readLine()) != null) {
                        buf.append(str);
                    }
                    fis.close();
                    json = buf.toString();
                    return new JSONObject(json);
                } catch (Exception ex) {
                    Log.e("Failed to read file", ex.toString());
                }
            } catch (FileNotFoundException ex) {
                Log.e("Failed to load file: file not found", file.toString());
            }
        } catch (Exception ex) {
            Log.e("Failed to find directory", ex.toString());
        }
        return null;
    }

    public static String readSettingsFile() {
        String data;
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/PuSSHd/");
            File file = new File(dir, FILENAME_SETTINGS);
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader fileRead = new InputStreamReader(fis);
                BufferedReader reader = new BufferedReader(fileRead);
                String str;
                StringBuilder buf = new StringBuilder();
                try {
                    while ((str = reader.readLine()) != null) {
                        buf.append(str);
                    }
                    fis.close();
                    data = buf.toString();
                    return data;
                } catch (Exception ex) {
                    Log.e("Failed to read file", ex.toString());
                }
            } catch (FileNotFoundException ex) {
                Log.e("Failed to load file: file not found", file.toString());
            }
        } catch (Exception ex) {
            Log.e("Failed to find directory", ex.toString());
        }
        return "";
    }
}
