package info.mattsaunders.apps.pusshd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

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

    static final String TARGET_DIR_NAME = "mnt/sdcard/Download";

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
        int port = Integer.parseInt(port_string);
        /*
        try {
            InetAddress ip = InetAddress.getByName(ip_string);
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
            InetAddress ip = null;
        }
        */

        //Log it:
        System.out.println("INITIALIZING: SSH SFTP: " + username_string +":"+ password_string + "@" + ip_string +":"+ port);

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
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
        sshd.setFileSystemFactory(getModifiedNativeFileSystemFactory());

        try {
            sshd.start();
            Log.i("SUCCESS: Server listening on port",Integer.toString(port));
            server_info.sshd = sshd;
            server_info.log = log;
        } catch (Exception ex) {
            Log.e("FAILURE: Server start failed", ex.toString());
            ex.printStackTrace();
        }


    }

    //Custom FileSystemFactory to change initial root directory on SFTP connection
    NativeFileSystemFactory getModifiedNativeFileSystemFactory() {
        return new NativeFileSystemFactory() {

            @Override
            public FileSystemView createFileSystemView(Session session) {
                String userName = getUsername(session);
                NativeFileSystemView nfsv = new ModifiedNativeFileSystemView(
                        userName, isCaseInsensitive());
                Log.d("Creating a modified NativeFileSystemView for user", nfsv.getUserName());
                return nfsv;
            }

        };
    }
    //Hook for testing without valid session
    String getUsername(Session session) {
        return session.getUsername();
    }
    //Class to return the altered FileSystemView with changed initial directory
    class ModifiedNativeFileSystemView extends NativeFileSystemView {
        String modifiedRootDir;

        public ModifiedNativeFileSystemView(String userName,boolean caseInsensitive) {
            super(userName, caseInsensitive);
            modifiedRootDir = System.getProperty("user.dir") + File.separator + TARGET_DIR_NAME;
            Log.d("Modified NativeFileSystemView created with root directory", modifiedRootDir);
        }

        @Override
        public SshFile getFile(String file) {
            return getFile(modifiedRootDir, file);
        }
    }

}
