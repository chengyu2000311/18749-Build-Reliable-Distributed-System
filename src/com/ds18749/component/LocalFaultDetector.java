package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.sql.Timestamp;
import java.util.logging.*;

import com.ds18749.component.Server.MessageType;

public class LocalFaultDetector {
    private Timer m_heartBeatTimer = null;
    private TimerTask m_heartBeatTimerTask;
    private Socket m_localFaultDetectorSocket;
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;
    private Logger m_Logger;


    public LocalFaultDetector(double heartBeatFrequency) {
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        m_heartBeatTimerTask = new TimerTask() {
            public void run() {
                sendHeartBeatToServer();
                listenToHeartBeatAnswerFromServer();
            }
        };
        this.m_heartBeatTimer = new Timer();
        Double period = 1.0 / heartBeatFrequency * 1000;
        m_heartBeatTimer.schedule(m_heartBeatTimerTask, 0, period.intValue());
    }

    private void sendHeartBeatToServer() {
        String msg = "LFD1_HEART_BEAT" + " " + Server.END_CHAR;

        // Log to console on the heartbeat message sent.
        m_Logger.log(Level.INFO, "LFD1 send out heart beat message to Server1: {0}", msg);

        // Open socket and write to the server.
        try {
            m_localFaultDetectorSocket = new Socket(serverIP, serverPortNumber);
            OutputStream out = m_localFaultDetectorSocket.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void listenToHeartBeatAnswerFromServer() {
        StringBuilder rawStringReceived = new StringBuilder();

        // Accept the socket and print received message from clients.
        try {
            m_localFaultDetectorSocket = new Socket(serverIP, serverPortNumber);
            InputStream in = m_localFaultDetectorSocket.getInputStream();

            // Store socket instream.
            for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                if(c ==-1)
                    break;
                rawStringReceived.append((char) c);
            }
            
            String receivedMessage = rawStringReceived.toString();
            String[] received = receivedMessage.toString().split(" ", 2);

            if (isHeartBeatAnswerFromServer(received[0])) {
                m_Logger.log(Level.INFO, "Heartbeat answered by from Server1: {0}\n", receivedMessage);
            } else {
            }
        } catch (Exception e) {
            m_Logger.log(Level.SEVERE, "LFD1 does not hear Heart Beat Answer from the Server1. Stop sending heart beats...\n");
            m_heartBeatTimer.cancel();
            // e.printStackTrace();
        }
    }

    private boolean isHeartBeatAnswerFromServer(String serverMessage) {
        return serverMessage.equals(MessageType.SERVER1_HEART_BEAT_ANSWER.name());
    }


    public static void main(String[] args) {
        LocalFaultDetector m_localFaultDetector = new LocalFaultDetector(0.5);
        System.out.printf("Localfault detector LFD1 has been launched at %s:%d\n", serverIP, serverPortNumber);
    }

}
