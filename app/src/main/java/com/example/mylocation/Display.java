package com.example.mylocation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class Display extends AppCompatActivity {
    private String deviceIpAddress = "0.0.0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        Bundle b = getIntent().getExtras();
        if(b != null){
            deviceIpAddress = b.getString(getString(R.string.device_ip));
        }

        if(deviceIpAddress == null){
            deviceIpAddress = "0.0.0.0";
        }
        displayInit();
    }

    /******************************************************************************************/

    private void displayInit(){

    }



}