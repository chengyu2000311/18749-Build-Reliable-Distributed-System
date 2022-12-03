package com.ds18749.component;

import util.IpPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;


public class Client{
    public static final String serverIP = "ece003.ece.local.cmu.edu";
    public static final int serverPortNumber = 4321;
    private final Lock timeLock = new ReentrantLock();
//    private final Lock serverTimeoutLock = new ReentrantLock();
    private static Timestamp lastTimeReceive;
    private static long timeout = 4 * 1000L;
    public static int clientStateCounter = 0;
    private static int serverID = 1;
    private final int clientId; 
    private Logger m_Logger;
    private int counter = 0;
    private List<IpPort> ServerIpPorts = Arrays.asList(
            new IpPort("ece000.ece.local.cmu.edu", 4301),
            new IpPort("ece001.ece.local.cmu.edu", 4302),
            new IpPort("ece002.ece.local.cmu.edu", 4303)
    );

    /**
     * Client default constructor.
     * @param id
     */
    public Client(int id) {
        this.clientId = id;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    /**
     * Client start to send messages to the server.
     */
    public void startSendingMessageToServer() {
        while (true) {
            clientStateCounter += 1;
//            boolean hasReceived = false;
            // for (int serverID=1; serverID<4; ++serverID) {
//            int serverID = curServerID;
            String messageString = "CLIENT" + clientId + "_DATA" + " " + counter + Server.END_CHAR;
            counter += 1;
            IpPort serverIpPort = ServerIpPorts.get(serverID-1);
            /* Open socket and write to server. */
            try (Socket client = new Socket(serverIpPort.IPAddress(), serverIpPort.portNumber())) {
                OutputStream out = client.getOutputStream();
                /* Log to console on the heartbeat message sent. */
                m_Logger.log(Level.INFO, String.format("Client%d sent out message to Server%d: %s", clientId, serverID, messageString));
                out.write(messageString.getBytes());
            } catch (Exception e) {
                if (!(e instanceof ConnectException)) {
                    e.printStackTrace();
                }
            }


            /* Listen for response from the server. */
//            hasReceived = listenToMessageResponseFromServer(serverID, hasReceived);
            listenToMessageResponseFromServer();



            /* Send message every 2 second. */
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // }
        }
    }

    public void start(){
        Thread clientThread = new Thread(this::startSendingMessageToServer);
        clientThread.start();

//        Thread client2Thread = new Thread(this::listenToMessageResponseFromServer);
//        client2Thread.start();

        Thread timeoutThread = new Thread(this::checkTimeout);
        timeoutThread.start();
    }

    /**
     * Listen to server's reponse to the message we (the client) send.
     */
    private void listenToMessageResponseFromServer() {
        StringBuilder rawStringReceived = new StringBuilder();

        // Accept the socket and print received message from clients.
        try {
            IpPort serverIpPort = ServerIpPorts.get(serverID-1);
            Socket clientSocket_= new Socket(serverIpPort.IPAddress(), serverIpPort.portNumber());
            InputStream in = clientSocket_.getInputStream();

            // Store socket instream.
            for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                if(c ==-1)
                    break;
                rawStringReceived.append((char) c);
            }
            
            String receivedMessage = rawStringReceived.toString();
            String[] received = receivedMessage.split(" ", 2);

//            timeLock.lock();
            // record the last receiving time
            lastTimeReceive = new Timestamp(System.currentTimeMillis());
//            m_Logger.log(Level.INFO, String.format("Pre: Received message answered by Server%d: %s\n", serverID, receivedMessage));
//            timeLock.unlock();

            m_Logger.log(Level.INFO, String.format("Received message answered by Server%d: %s\n", serverID, receivedMessage));
//            if (isMessageRespondFromServer(received[0])) {
//                m_Logger.log(Level.INFO, String.format("Received message answered by Server%d: %s\n", serverID, receivedMessage));
//            } else {
//                m_Logger.log(Level.INFO, "Discard Duplicate Response");
//            }
        } catch (Exception e) {
            if (!(e instanceof ConnectException)) {
                e.printStackTrace();
            }
        }
//        return true;
    }

    private void checkTimeout() {
        while (true) {
            boolean flag = false;
            timeLock.lock();
            Timestamp timeoutStamp = new Timestamp(System.currentTimeMillis() - timeout);
            if (lastTimeReceive != null && lastTimeReceive.before(timeoutStamp)) {
//                serverTimeoutLock.lock();
                serverID += 1;
                if(serverID == 4) serverID = 1;
                flag = true;
//                serverTimeoutLock.unlock();
//                m_Logger.log(Level.SEVERE, "TIMEOUT");
                System.out.println("TIMEOUT and current primary server is S" + serverID);
//                break;
            }
            timeLock.unlock();
            if (flag) break;
//            try {
//                Thread.sleep(3000L);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }

    /**
     * Check if current message is the server's reponse to our (the client's) message.
     * @param serverMessage
     * @return
     */
    private boolean isMessageRespondFromServer(String serverMessage) {
        return true; // serverMessage.equals(Server.eMessageType.SERVER1_MESSAGE_ANSWER.name());
    }


    /**
     * Main entry for client.
     * @param args
     */
    public static void main(String[] args) {
        /* Parse Client# from the terminal and construct client. */
        int clientId = Integer.parseInt(args[0]);
        Client m_client = new Client(clientId);
        System.out.printf("Client%d has been launched at %s:%d\n", clientId, serverIP, serverPortNumber);
//        m_client.startSendingMessageToServer();
        m_client.start();
    }
}