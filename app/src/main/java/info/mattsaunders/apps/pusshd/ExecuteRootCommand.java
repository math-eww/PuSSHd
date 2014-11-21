package info.mattsaunders.apps.pusshd;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute root command
 */
public class ExecuteRootCommand {
    static String line;
    static List<String> cmdOutput = new ArrayList<>();

    public static List<String> ExecCommand(String cmd) {
        try {
            cmd = cmd + "\n";
            Process process = Runtime.getRuntime().exec("su");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((cmd).getBytes());

            //after you exec everything that you want exit shell
            stdin.write("exit\n".getBytes());

            //flush and close the OutputStream
            stdin.flush();
            stdin.close();

            //read output and error of an executed command
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                Log.d("[Output]", line);
                if (line != null) { cmdOutput.add(line); }
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                //TODO: suppress verbose warnings here (ie netstat: showing only processes with your user ID)
                Log.e("[Error]", line);
                if (line != null) { cmdOutput.add(line); }
            }
            br.close();

            try {
                //finally, to destroy the process
                process.waitFor();
                process.destroy();
                return cmdOutput;
            } catch (InterruptedException ex) {
                Log.e("ROOT COMMAND: Failed to close process", ex.toString());
            }
        } catch (IOException ex) {
            Log.e("ROOT COMMAND: Caught IOException", ex.toString());
        }
        return cmdOutput;
    }
}
