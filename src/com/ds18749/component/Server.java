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
    public static final String serverIP = "10.0.0.145";
    public static final int serverPortNumber = 4321;

    private final String IPAddress;
    private final int portNumber;
    private ServerSocket m_ServerSocket; 
    private eServerState m_eServerState;
    private Logger m_Logger;

    /**
     * Message Type enum for the server to identify what kind of message it has received.
     * It is encoded as part of the message, for EXAMPLE:
     *      "CLIENT1_DATA Hello World!#"
     *      "LFD1_HEART_BEAT #" ...etc.
     */
    enum eMessageType {
        LFD1_HEART_BEAT,
        SERVER1_HEART_BEAT_ANSWER,
        SERVER1_MESSAGE_ANSWER,
        CLIENT1_DATA,
        CLIENT2_DATA,
        CLIENT3_DATA,
    };

    /**
     *  Server State. The server state will changed to RED, YELLOW, and BLUE 
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
    public Server(String ipAddress, int portNumber) {
        this.IPAddress = ipAddress;
        this.portNumber = portNumber;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    /**
     * Server starts service.
     */
    public void startService() {
        try {
            /* Socket construction. */
            InetAddress serverAddress = InetAddress.getByName(IPAddress);
            m_ServerSocket = new ServerSocket(portNumber, 10, serverAddress);

            /* Inifite loop, listen to the socket and receive any messages. */
            while (true) {
                receiveMessageFromSocket();            
            }
        } catch (Exception e) { // catch on new serversockect.
            e.printStackTrace();
        }
    }

    /**
     * Server read and identify peripheral message received from the socket.
     * @param m_serverSocket
     */
    private void receiveMessageFromSocket() {
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
            eMessageType receivedeMessageType = eMessageType.valueOf(received[0]);
            String receivedMessageContent = received[1];

            /* Answer to either Clients or the LFD. */
            answerToPeripheral(receivedeMessageType, receivedMessageContent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Respond to the LFD / server / client.
     * @param receivedeMessageType
     * @param receivedMessageContent
     */
    private void answerToPeripheral(eMessageType receivedeMessageType, String receivedMessageContent) {
        String serverReplyMessage = "";
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            if (receivedeMessageType == eMessageType.LFD1_HEART_BEAT) {
                m_Logger.log(Level.INFO, "S1 received Heart Beat from LFD1: {0}", receivedMessageContent);
                serverReplyMessage = eMessageType.SERVER1_HEART_BEAT_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Heart Beat Answer sent to LFD1 from S1: {0}\n", serverReplyMessage);
            } else if (receivedeMessageType == eMessageType.CLIENT1_DATA) {
                /* Change server state to RED. */
                m_eServerState = eServerState.RED;
                m_Logger.log(Level.INFO, "S1 received message from C1: {0}", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {0}", m_eServerState.name());
                serverReplyMessage = eMessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C1 from S1: {0}\n", serverReplyMessage);
            } else if (receivedeMessageType == eMessageType.CLIENT2_DATA) {
                /* Change server state to YELLOW. */
                m_eServerState = eServerState.YELLOW;
                m_Logger.log(Level.INFO, "S1 received message from C2: {0}", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {0}", m_eServerState.name());
                serverReplyMessage = eMessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C2 from S1: {0}\n", serverReplyMessage);
            } else if (receivedeMessageType == eMessageType.CLIENT3_DATA) {
                /* Change server state to BLUE. */
                m_eServerState = eServerState.BLUE;
                m_Logger.log(Level.INFO, "S1 received message from C3: {0}", receivedMessageContent);
                m_Logger.log(Level.INFO, "S1 current state: {0}", m_eServerState.name());
                serverReplyMessage = eMessageType.SERVER1_MESSAGE_ANSWER.name() + Server.END_CHAR;
                m_Logger.log(Level.INFO, "Message reply sent to C3 from S1: {0}\n", serverReplyMessage);
            }
            /* Write the server reply message to outstream. */
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
        System.out.printf("Server replica S1 has been launched at %s:%d\n\n", serverIP, serverPortNumber);
        Server m_server = new Server(serverIP, serverPortNumber);
        m_server.startService();
    }

}
