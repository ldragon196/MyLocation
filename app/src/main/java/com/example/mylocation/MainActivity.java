package com.example.mylocation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private UdpSocket udpSocket;
    private boolean deviceIsConnected = false;
    private String deviceIpAddress = "0.0.0.0";

    private TextView textLogMessage;
    private TextView textTeacherName, textTeacherS, textTeacherT, textTeacherStart;
    private TextView textStudentName, textStudentS, textStudentT, textStudentStart;
    private TextView textSpeed, textLatitude, textLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainComponentInit();
        startConnecting();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        udpSocket.stopReceive();
        connectionHandler.removeCallbacks(connectionRunnable);
    }

    /******************************************************************************************/

    private void mainComponentInit() {
        // Start UDP
        udpSocket = new UdpSocket(udpSocketHandler);

        textLogMessage = findViewById(R.id.text_view_message);
        textTeacherName = findViewById(R.id.text_view_teacher_name);
        textTeacherS = findViewById(R.id.text_view_teacher_s);
        textTeacherT = findViewById(R.id.text_view_teacher_t);
        textTeacherStart = findViewById(R.id.text_view_teacher_start);
        textStudentName = findViewById(R.id.text_view_student_name);
        textStudentS = findViewById(R.id.text_view_student_s);
        textStudentT = findViewById(R.id.text_view_student_t);
        textStudentStart = findViewById(R.id.text_view_student_start);
        textSpeed = findViewById(R.id.text_view_speed);
        textLatitude = findViewById(R.id.text_view_latitude);
        textLongitude = findViewById(R.id.text_view_longitude);
    }

    /******************************************************************************************/

    /* Send broadcast udp connection packet */
    private void broadcastConnection() {
        udpSocket.send(getString(R.string.connect_cmd), "255.255.255.255", Constants.UDP_PORT);
    }

    private void pingDevice() {
        udpSocket.send(getString(R.string.ping_cmd), deviceIpAddress, Constants.UDP_PORT);
    }

    /* Start, stop connection process */
    private void startConnecting() {
        // Check wifi connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(!isConnected){
            findViewById(R.id.panel_main_connecting).setVisibility(View.GONE);
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.wifi_enable)
                    .setPositiveButton(R.string.settings, (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        else{
            deviceIsConnected = false;
            findViewById(R.id.panel_main_connecting).setVisibility(View.VISIBLE);
            connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
        }
    }

    /* Timer for send and retry send connection packet. If no response -> stop */
    private int checkConnectCount = 0;

    private final Handler connectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionRunnable = new Runnable() {
        @Override
        public void run() {
            if(deviceIsConnected) {
                checkConnectCount++;
                if(checkConnectCount >= Constants.MAX_PING_ERROR_COUNT){
                    deviceIsConnected = false;
                    findViewById(R.id.panel_main_connecting).setVisibility(View.VISIBLE);
                    connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
                }
                else {
                    pingDevice();
                    connectionHandler.postDelayed(connectionRunnable, Constants.TIME_PING_DEVICE_MS);
                }
            }
            else {
                broadcastConnection();
                connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
            }
        }
    };

    /******************************************************************************************/

    private void handlePacketReceive(String message) {
        if(deviceIsConnected) {
            if(message.contains(getString(R.string.ok_cmd))) {
                checkConnectCount = 0;
            }
            else if(message.contains(getString(R.string.info_cmd))) {
                String[] info = message.split(",");
                if( (info.length == 12) && (info[0].equals(getString(R.string.info_cmd))) ) {
                    textTeacherName.setText(info[1]);
                    textTeacherS.setText(info[2]);
                    textTeacherT.setText(info[3]);
                    textTeacherStart.setText(info[4]);
                    textStudentName.setText(info[5]);
                    textStudentS.setText(info[6]);
                    textStudentT.setText(info[7]);
                    textStudentStart.setText(info[8]);
                    textSpeed.setText(info[9]);
                    textLatitude.setText(info[10]);
                    textLongitude.setText(info[11]);
                }
            }
            else if(message.contains(getString(R.string.error_cmd))) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        }
        else {
            if(message.contains(getString(R.string.ok_cmd))) {
                String[] cmd = message.split(",");
                if((cmd.length == 2) && (cmd[0].equals(getString(R.string.ok_cmd))) ) {
                    deviceIsConnected = true;
                    deviceIpAddress = cmd[1];
                    findViewById(R.id.panel_main_connecting).setVisibility(View.GONE);
                }
            }
        }
    }

    /******************************************************************************************/

    /* Handle message callback from udp socket */

    private final Handler udpSocketHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // Send packet success
                case UdpSocket.UDP_MESSAGE_WRITE:
                    udpSocket.startReceive();
                    break;

                // Receive packet success
                case UdpSocket.UDP_MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    handlePacketReceive(readMessage);
                    break;

                // UDP error
                case UdpSocket.UDP_ERROR:
                    Toast.makeText(getApplicationContext(), getString(R.string.udp_failure), Toast.LENGTH_SHORT).show();
                    deviceIsConnected = false;
                    findViewById(R.id.panel_main_connecting).setVisibility(View.VISIBLE);
                    connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
                    break;

                default:
                    break;
            }
        }
    };
}