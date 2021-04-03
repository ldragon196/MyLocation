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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainComponentInit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startConnecting();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConnecting();
        UdpSocket.getInstance().registerHandler(null);
    }

    /******************************************************************************************/

    private void mainComponentInit() {
        // Start UDP
        UdpSocket.getInstance().registerHandler(udpSocketHandler);

        // Button start connection
        Button buttonConnect = findViewById(R.id.button_main_connect);
        buttonConnect.setOnClickListener(v -> startConnecting());
    }

    /******************************************************************************************/

    /* Send broadcast udp connection packet */
    private void broadcastConnection() {
        UdpSocket.getInstance().send(getString(R.string.connect_cmd), "255.255.255.255", 33330);
    }

    /* Start, stop connection process */
    private void startConnecting() {
        // Check wifi connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(!isConnected){
            stopConnecting();
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.wifi_enable)
                    .setPositiveButton(R.string.settings, (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        else{
            timeRetryConnect = 0;
            findViewById(R.id.panel_main_connecting).setVisibility(View.VISIBLE);
            connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
        }
    }

    private void stopConnecting() {
        UdpSocket.getInstance().stopReceive();
        connectionHandler.removeCallbacks(connectionRunnable);
        findViewById(R.id.panel_main_connecting).setVisibility(View.GONE);
    }

    /* Timer for send and retry send connection packet. If no response -> stop */
    private int timeRetryConnect = 0;
    private final Handler connectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionRunnable = new Runnable() {
        @Override
        public void run() {
            timeRetryConnect++;
            if(timeRetryConnect >= Constants.MAX_RETRY_CONNECT_COUNT) {
                stopConnecting();
                Toast.makeText(getApplicationContext(), getString(R.string.cannot_scan), Toast.LENGTH_SHORT).show();
            }
            else {
                broadcastConnection();
            }
        }
    };

    /******************************************************************************************/

    private void handlePacketReceive(String message) {
        if(message.contains(getString(R.string.ok_cmd))) {
            String[] cmd = message.split(",");
            if((cmd.length == 2) && (cmd[0].equals(getString(R.string.ok_cmd))) ) {
                stopConnecting();

                Intent intent = new Intent(getApplicationContext(), Display.class);
                /* Active activity with params */
                Bundle b = new Bundle();
                b.putString(getString(R.string.device_ip), cmd[1]);
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
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
                    UdpSocket.getInstance().startReceive();
                    connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
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
                    stopConnecting();
                    break;

                default:
                    break;
            }
        }
    };
}