package com.example.mylocation;

import android.app.Application;
import android.os.Handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpSocket extends Application {
    private final int LocalPort = Constants.UDP_PORT;
    private Handler UdpHandler;
    private UdpThread UdpThread;
    public static final int UDP_MESSAGE_READ = 1, UDP_MESSAGE_WRITE = 2, UDP_ERROR = 3;

    private static UdpSocket instance;
    public static synchronized UdpSocket getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        UdpHandler = null;
        instance = this;
    }

    public void registerHandler(Handler handler){
        UdpHandler = handler;
    }

    public void startReceive() {
        UdpThread = new UdpThread();
        UdpThread.setReceiveFlag(true);
        UdpThread.start();
    }

    public void stopReceive() {
        if(UdpThread != null) {
            UdpThread.setReceiveFlag(false);
            UdpThread = null;
        }
    }

    public void send(String message, String address, int port) {
        if(UdpThread != null) {
            UdpThread.setReceiveFlag(false);
        }

        UdpThread = new UdpThread();
        UdpThread.write(message, address, port);
        UdpThread.start();
    }

    /******************************************************************************************/

    private class  UdpThread extends Thread {
        private DatagramSocket socket;
        private byte[] sendData;
        private boolean sendFlag = false;
        private boolean receiveFlag = false;
        private String desIpAddress;
        private int desPort;

        UdpThread() {
            if(socket == null) {
                try {
                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(LocalPort));
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override public void run() {
            if (sendFlag) {
                sendDataToServer();
                sendFlag = false;
            }
            while (receiveFlag) {
                receiveData();
            }
        }

        private void setReceiveFlag(boolean flag) {
            receiveFlag = flag;
        }

        private void write(String data, String address, int port) {
            desIpAddress = address;
            desPort = port;
            sendData = data.getBytes();
            sendFlag = true;
        }

        private void sendDataToServer() {
            try {
                InetAddress serverAddress = InetAddress.getByName(desIpAddress);
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, desPort);
                socket.send(packet);
                if(UdpHandler != null) {
                    UdpHandler.obtainMessage(UDP_MESSAGE_WRITE, packet.getLength(), 0, packet.getData()).sendToTarget();
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                if(UdpHandler != null){
                    UdpHandler.obtainMessage(UDP_ERROR, 0, 0, null).sendToTarget();
                }
            }
        }

        private void receiveData() {
            try {
                byte[] data = new byte[4096];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);

                if(UdpHandler != null){
                    UdpHandler.obtainMessage(UDP_MESSAGE_READ, packet.getLength(), 0, packet.getData()).sendToTarget();
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                if(UdpHandler != null){
                    UdpHandler.obtainMessage(UDP_ERROR, 0, 0, null).sendToTarget();
                }
            }
        }
    }
}
