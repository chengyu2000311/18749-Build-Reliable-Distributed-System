package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.*;

public class Server{
    public static final char END_CHAR = '#';
    private final String IPAddress;
    private final int portNumber;
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;
    private ServerSocket m_ServerSocket; 
    private ServerState m_ServerState;
    private Logger m_Logger;

    enum MessageType {
        LFD1_HEART_BEAT,
        SERVER1_HEART_BEAT_ANSWER,
        SERVER1_MESSAGE_ANSWER,
        CLIENT1_DATA,
        CLIENT2_DATA,
        CLIENT3_DATA,
    };

    enum ServerState {
        RED,
        YELLOW,
        BLUE
    };

    public Server(String IPAddress, int portNumber) {
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
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
                        String[] received = rawStringReceived.toString().split(" ", 2);
            MessageType receivedMessageType = MessageType.valueOf(received[0]);
            String receivedMessageContent = received[1];
            answerToPeripheral(receivedMessageType, receivedMessageContent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void answerToPeripheral(MessageType receivedMessageType, String receivedMessageContent) {
        String serverReplyMessage = "";
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            if (receivedMessageType == MessageType.LFD1_HEART_BEAT) {
                m_Logger.log(Level.INFO, "S1 received Heart Beat from LFD1: {0}\n", receivedMessageContent);
                serverReplyMessage = MessageType.SERVER1_HEART_BEAT_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Heart Beat Answer sent to LFD1 from S1: {0}\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT1_DATA) {
                m_ServerState = ServerState.RED;
                m_Logger.log(Level.INFO, "S1 received message from C1: {0}\n", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {}\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C1 from S1: {0}\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT2_DATA) {
                m_ServerState = ServerState.YELLOW;
                m_Logger.log(Level.INFO, "S1 received message from C2: {0}\n", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {}\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C2 from S1: {0}\n", serverReplyMessage);
            } else if (receivedMessageType == MessageType.CLIENT3_DATA) {
                m_ServerState = ServerState.BLUE;
                m_Logger.log(Level.INFO, "S1 received message from C3: {0}\n", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {0}\n", m_ServerState.name());
                serverReplyMessage = MessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C3 from S1: {0}\n", serverReplyMessage);
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
