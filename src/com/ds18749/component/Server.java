package com.ds18749.component;

import util.IpPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server{
    public static final char END_CHAR = '#';
    public static final String myIP = "ece00%d.ece.local.cmu.edu";
    public static final int myPortNumber = 430;

    private final String IPAddress;
    private final int portNumber;
    private final Lock serverStateLock = new ReentrantLock();
    private final long checkpointFrequency;
    private ServerSocket m_ServerSocket; 
    private eServerState m_eServerState;
    private Logger m_Logger;
    private int id;
    private boolean isPrimary;
    private int checkpointCount;

    private final List<IpPort> ServerIpPortS = Arrays.asList(
        new IpPort("ece000.ece.local.cmu.edu", 4301),
        new IpPort("ece001.ece.local.cmu.edu", 4302),
        new IpPort("ece002.ece.local.cmu.edu", 4303)
);

    /**
     * Message Type enum for the server to identify what kind of message it has received.
     * It is encoded as part of the message, for EXAMPLE:
     *      "CLIENT1_DATA Hello World!#"
     *      "LFD1_HEART_BEAT #" ...etc.
     */
    enum eMessageType {
        LFD_HEART_BEAT,
        HEART_BEAT_ANSWER,
        MESSAGE_ANSWER,
        CLIENT1_DATA,
        CLIENT2_DATA,
        CLIENT3_DATA,
        CHECKPOINT
    }

    /**
     *  Server State. The server state will change to RED, YELLOW, and BLUE
     *      when it receives message from S1, S2, S3 respectively.
     */
    enum eServerState {
        RED,
        YELLOW,
        BLUE
    };

    /**
     * Server default constructor.
     * @param ipAddress
     * @param portNumber
     */
    public Server(long checkpointFrequency, String ipAddress, int portNumber, int id, boolean isPrimary) {
        this.checkpointFrequency = checkpointFrequency;
        this.id = id;
        this.IPAddress = String.format(ipAddress, id-1);
        this.portNumber = portNumber * 10 + id;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        this.checkpointCount = 0;
        this.m_eServerState = eServerState.RED;
        this.isPrimary = isPrimary;
    }

    /**
     * Server starts service.
     */
    public void startService() {
        try {
            /* Socket construction. */
            InetAddress serverAddress = InetAddress.getByName(IPAddress);
            m_ServerSocket = new ServerSocket(portNumber, 10, serverAddress);
            
            if (this.isPrimary) {
                Thread checkpoint_thread = new Thread(this::sendCheckpointLoop);
                checkpoint_thread.start();
            }

            /* Infinite loop, listen to the socket and receive any messages. */
            while (true) {
                receiveMessageFromSocket();
            }
        } catch (Exception e) { // catch on new ServerSocket.
            e.printStackTrace();
        }

    }

    private void sendCheckpoint() {
        for (int i = 1; i < 4; i++) {
            if(id == i) continue;
            try {
                serverStateLock.lock();
                String checkpointMsg = String.format("CHECKPOINT STATE %s CHECKPOINT_COUNT %d%c", this.m_eServerState.name(), this.checkpointCount, Server.END_CHAR);
                serverStateLock.unlock();

                IpPort ipPort = ServerIpPortS.get(i-1);
                Socket replicaSocket = new Socket(ipPort.IPAddress(), ipPort.portNumber());
                OutputStream out = replicaSocket.getOutputStream();
                out.write(checkpointMsg.getBytes());
                /* Log to console on the checkpoint message sent. */
                m_Logger.log(Level.INFO, String.format("Primary replica send out checkpoint message to S%d: %s", i, checkpointMsg));
            } catch (Exception e) {

            }
        }
        this.checkpointCount ++;
    }

    private void sendCheckpointLoop() {
        while (true) {
            try {
                Thread.sleep(this.checkpointFrequency * 1000L);
                sendCheckpoint();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Server read and identify peripheral message received from the socket.
     */
    private void receiveMessageFromSocket() {
        while (true) {
            StringBuilder rawStringReceived = new StringBuilder();

            /* Accept the socket and print received message from clients. */
            try(Socket serverSocket = this.m_ServerSocket.accept()) {
                InputStream in = serverSocket.getInputStream();

                /* Record socket insteam. */
                for (int c = in.read(); c != END_CHAR; c = in.read()) {
                    rawStringReceived.append((char) c);
                }
                
                /* Parse the message received. */
                String[] received = rawStringReceived.toString().split(" ", 2);
                eMessageType receivedMessageType = eMessageType.valueOf(received[0]);
                String receivedMessageContent = received[1];
                /* Answer to either Clients or the LFD. */
                if (receivedMessageType == eMessageType.CHECKPOINT) {
                    reactToCheckpoint(receivedMessageContent);
                } else {
                    answerToPeripheral(receivedMessageType, receivedMessageContent);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }   

    }

    /**
     * Respond to the LFD / server / client.
     * Only answer to clients if the server is the primary replica
     * @param receivedMessageType
     * @param receivedMessageContent
     */
    private void answerToPeripheral(eMessageType receivedMessageType, String receivedMessageContent) {
        String serverReplyMessage = "";
        try (Socket serverSocket = m_ServerSocket.accept()){
            // if (receivedMessageType == eMessageType.CHECKPOINT) {
            //     /* If received a checkpoint message, change state and checkpoint counter but no reply */
            //     m_Logger.log(Level.INFO, String.format("S%d received checkpoint message from primary replica: %s", id, receivedMessageContent));
            //     String[] cpMsgBody = receivedMessageContent.split(" ", 4);
            //     this.m_eServerState = eServerState.valueOf(cpMsgBody[1]);
            //     this.checkpointCount = Integer.parseInt(cpMsgBody[3]);
            //     m_Logger.log(Level.INFO, String.format("After checkpoint, S%d state: %s, checkpoint counter: %d", id, m_eServerState.name(), this.checkpointCount));
            // } else {
                OutputStream out = serverSocket.getOutputStream();
                if (receivedMessageType == eMessageType.LFD_HEART_BEAT) {
                    m_Logger.log(Level.INFO, String.format("S%d received Heart Beat from LFD%d: %s", id, id, receivedMessageContent));
                    serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.HEART_BEAT_ANSWER.name(), id, Server.END_CHAR);
                    m_Logger.log(Level.INFO, String.format("Heart Beat Answer sent to LFD%d from S%d: %s\n", id, id, serverReplyMessage));
                } else if (receivedMessageType == eMessageType.CLIENT1_DATA) {
                    /* Change server state to RED. */
                    serverStateLock.lock();
                    m_eServerState = eServerState.RED;
                    m_Logger.log(Level.INFO, String.format("S%d received message from C1: %s", id, receivedMessageContent));
                    m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                    serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.MESSAGE_ANSWER.name(), id, Server.END_CHAR);
                    m_Logger.log(Level.INFO, String.format("Message reply sent to C1 from S%d: %s\n", id, serverReplyMessage));
                    serverStateLock.unlock();
                    if(isPrimary == false){
                        isPrimary = true;
//                        System.out.println("I am the primary server");
                        m_Logger.log(Level.INFO, "current server is the primary server!");
                        Thread checkpoint_thread = new Thread(this::sendCheckpointLoop);
                        checkpoint_thread.start();
                    }else{
                        m_Logger.log(Level.INFO, "isPrimary: true");
                    }
                } else if (receivedMessageType == eMessageType.CLIENT2_DATA) {
                    /* Change server state to YELLOW. */
                    serverStateLock.lock();
                    m_eServerState = eServerState.YELLOW;
                    m_Logger.log(Level.INFO, String.format("S%d received message from C2: %s", id, receivedMessageContent));
                    m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                    serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.MESSAGE_ANSWER.name(), id, Server.END_CHAR);
                    m_Logger.log(Level.INFO, String.format("Message reply sent to C2 from S%d: %s\n", id, serverReplyMessage));
                    serverStateLock.unlock();
                    if(isPrimary == false){
                        isPrimary = true;
//                        System.out.println("I am the primary server");
                        m_Logger.log(Level.INFO, "current server is the primary server!");
                        Thread checkpoint_thread = new Thread(this::sendCheckpointLoop);
                        checkpoint_thread.start();
                    }else{
                        m_Logger.log(Level.INFO, "isPrimary: true");
                    }
                } else if (receivedMessageType == eMessageType.CLIENT3_DATA) {
                    /* Change server state to BLUE. */
                    serverStateLock.lock();
                    m_eServerState = eServerState.BLUE;
                    m_Logger.log(Level.INFO, String.format("S%d received message from C3: %s", id, receivedMessageContent));
                    m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                    serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.HEART_BEAT_ANSWER.name(), id, Server.END_CHAR); ;
                    m_Logger.log(Level.INFO, String.format("Message reply sent to C3 from S%d: %s\n", id, serverReplyMessage));
                    serverStateLock.unlock();
                    if(isPrimary == false){
                        isPrimary = true;
//                        System.out.println("I am the primary server");
                        m_Logger.log(Level.INFO, "current server is the primary server!");
                        Thread checkpoint_thread = new Thread(this::sendCheckpointLoop);
                        checkpoint_thread.start();
                    }else{
                        m_Logger.log(Level.INFO, "isPrimary: true");
                    }
                }
                /* Write the server reply message to outstream. */
                out.write(serverReplyMessage.getBytes());
            // }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void reactToCheckpoint(String receivedMessageContent) {
        /* If received a checkpoint message, change state and checkpoint counter but no reply */
        m_Logger.log(Level.INFO, String.format("S%d received checkpoint message from primary replica: %s", id, receivedMessageContent));
        String[] cpMsgBody = receivedMessageContent.split(" ", 4);
        this.m_eServerState = eServerState.valueOf(cpMsgBody[1]);
        this.checkpointCount = Integer.parseInt(cpMsgBody[3]);
        m_Logger.log(Level.INFO, String.format("After checkpoint, S%d state: %s, checkpoint counter: %d", id, m_eServerState.name(), this.checkpointCount));
    }
    
    /**
     * Server program entrance.
     * @param args
     */
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        System.out.printf("Server replica S%d has been launched at %s:%d\n\n", id, String.format(myIP, id), myPortNumber * 10 + id);
        Server m_server;
//        if (id == 1) {
//            m_server = new Server(5, myIP, myPortNumber, id, true);
//        } else {
//            m_server = new Server(5, myIP, myPortNumber, id, false);
//        }
        m_server = new Server(5, myIP, myPortNumber, id, false);
        m_server.startService();
    }

}
