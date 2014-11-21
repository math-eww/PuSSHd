package info.mattsaunders.apps.pusshd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.util.Arrays;
import java.util.List;

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

    static final String TARGET_DIR_NAME = "mnt/sdcard";
    private static final String FILENAME = "ssh_pid";

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
            Log.i("SUCCESS: Server listening on port", Integer.toString(port));
            server_info.sshd = sshd;
            server_info.log = log;
            if (server_info.suEnabled) {
                try {
                    //TODO: Stress test this way of scraping PID - what if the first line is not what is expected?
                    List<String> cmdOut = ExecuteRootCommand.ExecCommand("busybox netstat -lp | grep " + port_string);
                    int pid = Integer.parseInt(cmdOut.get(0).split("LISTEN|/")[1].trim());
                    server_info.sshdPid = pid;
                    Log.i("SSHD PID", Integer.toString(pid));
                    writePidFile(pid);
                } catch (Exception ex) {
                    Log.e("Failed to get SSHD PID", ex.toString());
                }
            }
        }catch (BindException bex) {
            Log.e("FAILURE: Port already in use", bex.toString());
            bex.printStackTrace();
            //TODO: This works to check PID against old pid, but is PID of app not just SSHD process (even thought it launches a new service?)
            //TODO: So we need to figure out a different way to handle when the port is blocked - maybe just relaunching the application? Is there a method to unbind?
            //TODO: Maybe reconsider attempting to store reference to sshd object so we can stop it with it's stop method, even after application reload?
            //Here we check the PID of the process blocking the port against the stored PID from the last launch of SSHD
            if (server_info.suEnabled) {
                Log.i("Attempting to check if old SSHD PID matched PID blocking port", port_string);
                try {
                    List<String> cmdOut = ExecuteRootCommand.ExecCommand("busybox netstat -lp | grep " + port_string);
                    int pid = Integer.parseInt(cmdOut.get(0).split("LISTEN|/")[1].trim());
                    server_info.sshdPid = pid;
                    Log.i("Blocking PID = ", Integer.toString(pid));
                    int oldPid = readPidFile();
                    Log.i("Old PID = ", Integer.toString(oldPid));
                    if (pid == oldPid) {
                        ExecuteRootCommand.ExecCommand("kill -9 " + pid);
                        sshd.start();
                    }
                } catch (Exception ex) {
                    Log.e("Failed to get Blocking PID", ex.toString());
                }
            }
        } catch (Exception ex) {
            Log.e("FAILURE: Server start failed", ex.toString());
            ex.printStackTrace();
        }


    }

    public static void writePidFile(int data) {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/PuSSHd/");
            dir.mkdir();
            File file = new File(dir, FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(Integer.toString(data).getBytes());
                //Log.i("Successfully wrote JSON to file", data.toString());
            } catch (Exception ex) {
                Log.e("Failed to save pid", Integer.toString(data));
                ex.printStackTrace();
            }
            fos.close();
        } catch (Exception ex) {
            Log.e("Failed to open file", FILENAME);
            ex.printStackTrace();
        }
    }

    public static int readPidFile() {
        String pid;
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
                    pid = buf.toString();
                    return Integer.parseInt(pid);
                } catch (Exception ex) {
                    Log.e("Failed to read file", ex.toString());
                }
            } catch (FileNotFoundException ex) {
                Log.e("Failed to load file: file not found", file.toString());
            }
        } catch (Exception ex) {
            Log.e("Failed to find directory", ex.toString());
        }
        return 0;
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
