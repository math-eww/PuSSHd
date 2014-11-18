package info.mattsaunders.apps.pusshd;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.widget.TextView;
import android.widget.ToggleButton;


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

    public static Context getAppContext(){
        return server_info.context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        //Init context variable
        server_info.context = getApplicationContext();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

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
            return rootView;
        }
    }

    public static void setupServerToggle(View v) {
        //Set button label:
        final ToggleButton serverToggle = (ToggleButton)v.findViewById(R.id.serverToggle);
        serverToggle.setText("Start Server");

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
        final EditText inputUser = (EditText) v.findViewById(R.id.user);
        final EditText inputPass = (EditText) v.findViewById(R.id.pass);
        final EditText inputPort = (EditText) v.findViewById(R.id.port);

        //Set button listener:
        serverToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //Get user's input:
                    String username = inputUser.getText().toString();
                    String password = inputPass.getText().toString();
                    String port = inputPort.getText().toString();
                    //String ip = getLocalIpAddress();

                    //Server starting: set labels correctly:
                    connectionInfo.setText(username + ":" + password + "@");
                    ipaddress.setText(ip + ":" + port);
                    //Write to log:
                    System.out.println("Server Starting: " + username + ":" + password + "@" + ipaddress + ":" + port);
                    //Start the server:
                } else {
                    //Server stopping: set labels correctly:
                    connectionInfo.setText("");
                    ipaddress.setText(ip);
                    //Write to log:
                    System.out.println("Server Stopping: ||||||||||||||||||||||||||||||||||||");
                    //Stop the server:
                }
            }
        });
    }

    public static String getIpAddr() {
        try {
            Context c = getAppContext();
            WifiManager wifiManager = (WifiManager) c.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();

            String ipString = String.format(
                    "%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));

            return ipString;
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
            return "000.000.0.0";
        }
    }
}
