package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Server{
    public static final char END_CHAR = '#';
    private final String IPAddress;
    private final int portNumber;
    private Map<Integer, Integer> record = new HashMap<>();
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;
    private ServerSocket m_ServerSocket; 

    enum MessageType {
        LFD1_HEART_BEAT,
        SERVER1_HEART_BEAT_ANSWER,
        SERVER1_MESSAGE_ANSWER,
        CLIENT0_DATA,
        CLIENT1_DATA,
        CLIENT2_DATA,
    };

    public Server(String IPAddress, int portNumber) {
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }

    public void startService() {
        try {
            InetAddress serverAddress = InetAddress.getByName(IPAddress);
            try {
                m_ServerSocket = new ServerSocket(portNumber, 10, serverAddress);
                while (true) {
                    receiveMessageFromSocket();            
                }
            } catch (Exception e) { // catch on service.accept()
                e.printStackTrace();
            }
        } catch (UnknownHostException e) { // catch on new serversockect.
            e.printStackTrace();
        }
    }

    /**
     * Server read and identify peripheral message received from the socket.
     * @param m_serverSocket
     */
    private void receiveMessageFromSocket() {
        StringBuilder rawStringReceived = new StringBuilder();

        // Accept the socket and print received message from clients.
        try(Socket serverSocket = this.m_ServerSocket.accept()) {
            InputStream in = serverSocket.getInputStream();

            // Store socket instream.
            for (int c = in.read(); c != END_CHAR; c = in.read()) {
                if(c ==-1)
                    break;
                rawStringReceived.append((char) c);
            }
            
            String receivedMessage = rawStringReceived.toString();
            // System.out.printf("Received: %s", receivedMessage);

            String[] received = receivedMessage.toString().split(" ", 2);

            if (isLFDHeartBeat(received[0])) {
                System.out.printf("Heartbeat received from LFD1: %s\n", receivedMessage);
                answerHeartBeat();
            } else if (isClient0Message(received[0])) {
                System.out.printf("Message received from client 0: %s\n", received[1]);
            } else if (isClient1Message(received[0])) {
                System.out.printf("Message received from client 1: %s\n", received[1]);
            } else if (isClient2Message(received[0])) {
                System.out.printf("Message received from client 2: %s\n", received[1]);
            }
            /*
            
            if (sign.equals("plus")) {
                record.put(idNum, record.getOrDefault(idNum, 0) + 1);
            } else {
                record.put(idNum, record.getOrDefault(idNum, 0) - 1);
            }
            System.out.printf("Received message from client%d, and current record is %s%n", idNum, record.toString());
            */
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private boolean isLFDHeartBeat(String receivedMessage) {
        return (receivedMessage.equals(MessageType.LFD1_HEART_BEAT.name()));
    }
    
    private boolean isClient0Message(String receivedMessage) {
        return receivedMessage.equals(MessageType.CLIENT0_DATA.name());
    }

    private boolean isClient1Message(String receivedMessage) {
        return receivedMessage.equals(MessageType.CLIENT1_DATA.name());
    }
    
    private boolean isClient2Message(String receivedMessage) {
        return receivedMessage.equals(MessageType.CLIENT2_DATA.name());
    }

    private void answerHeartBeat() {
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            String msg = "SERVER1_HEART_BEAT_ANSWER" + Server.END_CHAR;
            out.write(msg.getBytes());
            System.out.printf("Heart Beat Answer send to LFD1 from Server1: %s\n", msg);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void answerClientMessage() {
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            String msg = "SERVER1_MESSAGE_ANSWER" + Server.END_CHAR;
            out.write(msg.getBytes());
            System.out.printf("Heart Beat Answer send to LFD1 from Server1: %s\n", msg);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Server program entrance.
     * @param args
     */
    public static void main(String[] args) {
        System.out.printf("Server replica S1 has been launched at %s:%d\n", serverIP, serverPortNumber);
        Server m_server = new Server(serverIP, serverPortNumber);
        m_server.startService();
    }

}
