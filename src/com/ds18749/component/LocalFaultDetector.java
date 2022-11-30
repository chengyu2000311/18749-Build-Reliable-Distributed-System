package com.ds18749.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

public class LocalFaultDetector {
    public static String serverIP = "ece00%d.ece.local.cmu.edu";
    public static int serverPortNumber = 430;
    public static String myIP = "ece00%d.ece.local.cmu.edu";
    public static int myPortNumber = 432;

    private Timestamp lastHeartBeat;
    private final Lock lastHeartBeatLock = new ReentrantLock();
    private final Lock serverTimeoutLock = new ReentrantLock();
    private long timeout = 4 * 1000L;
    private boolean serverTimeout = false;
    private final int id;
    private boolean serverAlive = false;
    private final Logger m_Logger;
    private final int heartBeatFrequency;
    private ServerSocket m_GDBHeartBeat;



    /**
     * Local Fault Detector (LFD) parameterized constructor.
     * @param heartBeatFrequency freq
     */
    public LocalFaultDetector(int heartBeatFrequency, int id) {
        this.heartBeatFrequency = heartBeatFrequency;
        this.id = id;
        this.myIP = String.format(myIP, id-1);
        this.serverIP = String.format(serverIP, id-1);
        this.serverPortNumber = this.serverPortNumber * 10 + id;
        this.myPortNumber = this.myPortNumber * 10 + id;
        /* Initialize the logger. */
        this.m_Logger = Logger.getLogger("LocalFaultDetector");
        /* start detecting fault of servers */
    }

    public void start() {
        try {
            InetAddress serverAddress = InetAddress.getByName(myIP);
            this.m_GDBHeartBeat = new ServerSocket(myPortNumber, 10, serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread serverThread = new Thread(this::listenToHeartBeatFromGDB);
        serverThread.start();
        Thread timeoutThread = new Thread(this::checkTimeout);
        timeoutThread.start();

        /* Setup timer for sending LFD heartbeat. */
        while (true) {
            try {
                Thread.sleep(heartBeatFrequency * 1000L);
                heartBeatWithServer();
            } catch (Exception e) {
            }
            serverTimeoutLock.lock();
            if (serverTimeout) {
                break;
            }
            serverTimeoutLock.unlock();
        }
    }

    /**
     * Check if timeout occurs
     */
    private void checkTimeout() {
        while (true) {
            lastHeartBeatLock.lock();
            Timestamp timeoutStamp = new Timestamp(System.currentTimeMillis() - timeout);
            if (lastHeartBeat != null && lastHeartBeat.before(timeoutStamp)) {
                serverTimeoutLock.lock();
                serverTimeout = true;
                serverTimeoutLock.unlock();
                m_Logger.log(Level.SEVERE, "TIMEOUT. LFD{0} does not hear Heart Beat Answer from the Server{0}. Stop sending heart beats...", id);
                deregisterMember();
                break;
            }
            lastHeartBeatLock.unlock();
        }
    }

    /**
     * Periodically send a heart beat message to server through socket.
     */
    private void heartBeatWithServer() throws IOException {
        /* Example: LFD1_HEART_BEAT */
        String msg = String.format("LFD_HEART_BEAT %d%c", id, Server.END_CHAR);
        /* Log to console on the heartbeat message sent. */
        m_Logger.log(Level.INFO, String.format("LFD%d send out heart beat message to Server%d: %s", id, id, msg));
        /* Open socket and write to the server. */
        Socket m_localFaultDetectorSocket = new Socket(serverIP, serverPortNumber);
        OutputStream out = m_localFaultDetectorSocket.getOutputStream();
        out.write(msg.getBytes());

        StringBuilder rawStringReceived = new StringBuilder();

        m_localFaultDetectorSocket = new Socket(serverIP, serverPortNumber);
        InputStream in = m_localFaultDetectorSocket.getInputStream();
        for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
            if (c == -1) {
                break;
            }
            rawStringReceived.append((char) c);
        }

        String receivedMessage = rawStringReceived.toString();
        if (receivedMessage.startsWith(String.format("Server%d_%s", id, Server.eMessageType.HEART_BEAT_ANSWER.name()))) {
            m_Logger.log(Level.INFO, String.format("Heartbeat answered by from Server%d: %s\n", id, receivedMessage));
            lastHeartBeatLock.lock();
            lastHeartBeat = new Timestamp(System.currentTimeMillis());
            lastHeartBeatLock.unlock();

            if (!serverAlive) {
                serverAlive = true;
                registerMember();
            }
        }

    }

    /**
     * Read from the socket a responded Heart Beat Answer from the Server.
     */
    private void listenToHeartBeatAnswerFromGFD() {

        StringBuilder rawStringReceived = new StringBuilder();
        try (Socket serverSocket = m_GDBHeartBeat.accept()) {
            InputStream in = serverSocket.getInputStream();
            /* Record socket insteam. */
            for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                if (c == -1) {
                    break;
                }
                rawStringReceived.append((char) c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(rawStringReceived.toString());

        if (rawStringReceived.toString().startsWith(GlobalFaultDetector.eMessageType.GDB_HEART_BEAT.name())) {
            String serverReplyMessage = String.format("%s %d%c", GlobalFaultDetector.eMessageType.GDB_HEART_BEAT_ANSWER, id, Server.END_CHAR);
            try (Socket serverSocket = m_GDBHeartBeat.accept()) {
                OutputStream out = serverSocket.getOutputStream();
                m_Logger.log(Level.INFO, String.format("LDF%d received Heart Beat from GFD: %s", id, rawStringReceived.toString()));
                out.write(serverReplyMessage.getBytes());
                m_Logger.log(Level.INFO, String.format("Heart Beat Answer sent to GDF from LDF%d: %s\n", id, serverReplyMessage));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param out
     * @param received
     * answer based on what is recceived
     */
    private void answerToPeripheral(OutputStream out, String received) {
        if (received.equals(GlobalFaultDetector.eMessageType.GDB_HEART_BEAT.name())) {
            try {
                out.write(String.format("%s %d%c", GlobalFaultDetector.eMessageType.GDB_HEART_BEAT_ANSWER, id, Server.END_CHAR).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Register member with GFD
     */
    private void registerMember() {
        /* Example: LFD1_HEART_BEAT */
        String msg = String.format("LFD%d: add replica S%d%c", id, id, Server.END_CHAR);
        /* Log to console on the heartbeat message sent. */
        m_Logger.log(Level.INFO, String.format("LFD%d send out register message to GDB: %s", id, msg));

        /* Open socket and write to the server. */
        try {
            Socket m_globalFaultDetectorSocket = new Socket(GlobalFaultDetector.myIP, GlobalFaultDetector.myPortNumber);
            OutputStream out = m_globalFaultDetectorSocket.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Deregister member with GDF
     */
    private void deregisterMember() {
        /* Example: LFD1_HEART_BEAT */
        String msg = String.format("LFD%d: delete replica S%d%c", id, id, Server.END_CHAR);
        /* Log to console on the heartbeat message sent. */
        m_Logger.log(Level.INFO, String.format("LFD%d send out register message to GDB: %s", id, msg));

        /* Open socket and write to the server. */
        try {
            Socket m_globalFaultDetectorSocket = new Socket(GlobalFaultDetector.myIP, GlobalFaultDetector.myPortNumber);
            OutputStream out = m_globalFaultDetectorSocket.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){

        }
    }

    /**
     * continously listen to heartbeat
     */
    private void listenToHeartBeatFromGDB() {
        while (true) {
            listenToHeartBeatAnswerFromGFD();
        }
    }

    /**
     * Main entry for LFD instance.
     * @param args with id
     */
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        LocalFaultDetector m_localFaultDetector = new LocalFaultDetector(2, id);
        m_localFaultDetector.start();
        System.out.printf("LocalFault detector LFD has been launched at %s:%d\n", myIP, myPortNumber);
    }

}
