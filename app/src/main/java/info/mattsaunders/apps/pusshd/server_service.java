package info.mattsaunders.apps.pusshd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.mattsaunders.apps.pusshd.sshd.PseudoTerminalFactory;
import info.mattsaunders.apps.pusshd.sshd.SimplePasswordAuthenticator;
import info.mattsaunders.apps.pusshd.sshd.SimplePublicKeyAuthenticator;

/**
 * Service class that actually runs the code continuously
 */
public class server_service extends IntentService {

    public server_service() {
        super("server_service");
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "SSH Service Starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        Bundle extras = workIntent.getExtras();
        String ip_string = extras.getString("IP");
        String port_string = extras.getString("PORT");
        String username_string = extras.getString("USER");
        String password_string = extras.getString("PASS");

        //Process data
        /*
        try {
            InetAddress ip = InetAddress.getByName(ip_string);
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
            InetAddress ip = null;
        }
        */
        int port = Integer.parseInt(port_string);

        //Log it:
        System.out.println("ENTERING SERVICE: SSH SFTP BEGIN: " + username_string +":"+ password_string + "@" + ip_string +":"+ port);

        //Initialize the server:
        final Logger log = LoggerFactory.getLogger(server_service.class);
        final SshServer sshd = SshServer.setUpDefaultServer();
        final SimplePasswordAuthenticator passwordAuth = new SimplePasswordAuthenticator();
        final SimplePublicKeyAuthenticator publicKeyAuth = new SimplePublicKeyAuthenticator();

        passwordAuth.setUser(username_string);
        passwordAuth.setPassword(password_string);
        sshd.setPort(port);

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(server_info.getAppContext().getFilesDir().getPath() + "/key.ser"));
        sshd.setShellFactory(new PseudoTerminalFactory("/system/bin/sh", "-i"));
        sshd.setPasswordAuthenticator(passwordAuth);
        sshd.setPublickeyAuthenticator(publicKeyAuth);

        System.out.println("ABOUT TO TRY TO START SSHD....");
        try {
            //final Runtime runtime = Runtime.getRuntime();
            //runtime.exec("su");
            sshd.start();
            Log.e("SSHD start success on port",Integer.toString(port));
        } catch (Exception ex) {
            Log.e("SSHD start failure", ex.toString());
            ex.printStackTrace();
        }


    }
}
