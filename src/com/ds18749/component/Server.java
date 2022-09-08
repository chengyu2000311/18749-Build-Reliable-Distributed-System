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
    private ServerState m_ServerState;

    enum MessageType {
        LFD1_HEART_BEAT,
        SERVER1_HEART_BEAT_ANSWER,
        SERVER1_MESSAGE_ANSWER,
        CLIENT0_DATA,
        CLIENT1_DATA,
        CLIENT2_DATA,
    };

    enum ServerState {
        RED,
        YELLOW,
        BLUE
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
            
            
            // System.out.printf("Received: %s", receivedMessage);

            String[] received = rawStringReceived.toString().split(" ", 2);
            MessageType receivedMessageType = MessageType.valueOf(received[0]);
            String receivedMessageContent = received[1];
            answerToPeripheral(receivedMessageType, receivedMessageContent);
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

    private void answerToPeripheral(MessageType receivedMessageType, String receivedMessageContent) {
        String serverReplyMessage = "";
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            if (receivedMessageType == MessageType.LFD1_HEART_BEAT) {
                System.out.printf("Server1 received Heart Beat from LFD1: %s\n", receivedMessageContent);
                serverReplyMessage = MessageType.SERVER1_HEART_BEAT_ANSWER.name() + Server.END_CHAR;
                System.out.printf("Heart Beat Answer sent to LFD1 from Server1: %s\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT0_DATA) {
                m_ServerState = ServerState.RED;
                System.out.printf("Server1 received message from Client0: %s\n", receivedMessageContent);
                System.out.printf("Server1 current state: %s\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                System.out.printf("Message reply sent to Client0 from Server1: %s\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT0_DATA) {
                m_ServerState = ServerState.YELLOW;
                System.out.printf("Server1 received message from Client1: %s\n", receivedMessageContent);
                System.out.printf("Server1 current state: %s\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                System.out.printf("Message reply sent to Client1 from Server1: %s\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT0_DATA) {
                m_ServerState = ServerState.BLUE;
                System.out.printf("Server1 received message from Client2: %s\n", receivedMessageContent);
                System.out.printf("Server1 current state: %s\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                System.out.printf("Message reply sent to Client2 from Server1: %s\n", serverReplyMessage);
            }
            
            out.write(serverReplyMessage.getBytes());
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
