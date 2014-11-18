package info.mattsaunders.apps.pusshd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

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
        final String user = username_string;
        final String pass = password_string;

        //Log it out:
        System.out.println("ENTERING SERVICE: SSH SFTP BEGIN: " + user +":"+ pass + "@" + ip_string +":"+ port);

        //Initialize the server:
        final Logger log = LoggerFactory.getLogger(server_service.class);
        final SshServer sshd = SshServer.setUpDefaultServer();

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
                "key.ser"));

        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthPassword.Factory());
        sshd.setUserAuthFactories(userAuthFactories);

        sshd.setCommandFactory(new ScpCommandFactory());

        List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
        namedFactoryList.add(new SftpSubsystem.Factory());
        sshd.setSubsystemFactories(namedFactoryList);

        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                return user.equals(username) && pass.equals(password);
            }
        });

        sshd.setPort(port);

        System.out.println("ABOUT TO START SSHD....");

        try {
            //Runtime.getRuntime().exec("su");
            final Runtime runtime = Runtime.getRuntime();
            runtime.exec("su");
            runtime.exec("su sshd.start()");
            //sshd.start();
            log.info("SSHD is started.");
            System.out.println(sshd.getHost());
            System.out.println(sshd.getPort());
        } catch (Exception ex) {
            Log.e("SSHD start", ex.toString());
            ex.printStackTrace();
        }


    }
}
