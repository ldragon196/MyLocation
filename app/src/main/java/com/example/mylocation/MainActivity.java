package com.example.mylocation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private UdpSocket udpSocket;

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
        connectionHandler.removeCallbacks(connectionRunnable);
    }

    /******************************************************************************************/

    private void mainComponentInit() {
        // Start UDP
        udpSocket = new UdpSocket(udpSocketHandler, 33330);

        // Button start connection
        Button buttonConnect = findViewById(R.id.button_main_connect);
        buttonConnect.setOnClickListener(v -> startConnecting());

        // Check wifi connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(!isConnected){
            Toast.makeText(getApplicationContext(), "Please connect to wifi before", Toast.LENGTH_SHORT).show();
        }
    }

    /******************************************************************************************/

    /* Send broadcast udp connection packet */
    private void broadcastConnection() {
        udpSocket.send(getString(R.string.connect_cmd), "255.255.255.255", 33300);
    }

    /* Start connection process */
    private void startConnecting() {
        timeRetryConnect = 0;
        findViewById(R.id.panel_main_connecting).setVisibility(View.VISIBLE);
        broadcastConnection();
    }

    /* Timer for send and retry send connection packet. If no response -> stop */
    private int timeRetryConnect = 0;
    private final Handler connectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionRunnable = new Runnable() {
        @Override
        public void run() {
            timeRetryConnect++;
            if(timeRetryConnect > Constants.MAX_RETRY_CONNECT_COUNT) {
                udpSocket.stopReceive();
                connectionHandler.removeCallbacks(connectionRunnable);
                findViewById(R.id.panel_main_connecting).setVisibility(View.GONE);
            }
            else {
                broadcastConnection();
            }
        }
    };

    /******************************************************************************************/

    /* Handle message callback from udp socket */
    private final Handler udpSocketHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // Send packet success
                case UdpSocket.UDP_MESSAGE_WRITE:
                    udpSocket.startReceive();
                    connectionHandler.postDelayed(connectionRunnable, Constants.TIME_RETRY_CONNECT_MS);
                    break;

                // Receive packet success
                case UdpSocket.UDP_MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                    break;

                // UDP error
                case UdpSocket.UDP_ERROR:
                    Toast.makeText(getApplicationContext(), getString(R.string.udp_failure), Toast.LENGTH_SHORT).show();
                    findViewById(R.id.panel_main_connecting).setVisibility(View.GONE);
                    break;

                default:
                    break;
            }
        }
    };
}