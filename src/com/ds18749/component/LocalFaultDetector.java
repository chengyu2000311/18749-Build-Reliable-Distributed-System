package com.ds18749.component;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class LocalFaultDetector {
    private final String[] messages = new String[]{"plus", "minus"};
    private Timer m_heartBeatTimer = null;
    private TimerTask m_heartBeatTimerTask;
    private final int m_heartBeatFrequency;
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;


    public LocalFaultDetector(int heartBeatFrequency) {
        this.m_heartBeatFrequency = heartBeatFrequency;

        m_heartBeatTimerTask = new TimerTask() {
            public void run() {
                sendHeartBeatToServer();
            }
        };
        this.m_heartBeatTimer = new Timer();
        m_heartBeatTimer.schedule(m_heartBeatTimerTask, 0, 1/heartBeatFrequency * 1000);
    }

    private void sendHeartBeatToServer() {
        String msg = "<LFD1_HEART_BEAT>" + Server.END_CHAR;

        // Log to console on the heartbeat message sent.
        System.out.printf("LFD1 send out heart beat message to server 1: %s", msg);

        // Open socket and write to the server.
        try (Socket m_localFaultDetectorSocket = new Socket(serverIP, serverPortNumber)) {
            OutputStream out = m_localFaultDetectorSocket.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Local fault detctor started\n");
        LocalFaultDetector m_localFaultDetector = new LocalFaultDetector(1);
    }

}
