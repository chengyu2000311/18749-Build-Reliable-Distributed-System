package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.*;

public class Server{
    public static final char END_CHAR = '#';
    public static String myIP = "ece00%d.ece.local.cmu.edu";
    public static final int myPortNumber = 430;

    private final String IPAddress;
    private final int portNumber;
    private ServerSocket m_ServerSocket; 
    private eServerState m_eServerState;
    private Logger m_Logger;
    private int id;

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
    public Server(String ipAddress, int portNumber, int id) {
        this.id = id;
        this.IPAddress = String.format(ipAddress, id-1);
        this.portNumber = portNumber * 10 + id;
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

            /* Infinite loop, listen to the socket and receive any messages. */
            while (true) {
                receiveMessageFromSocket();            
            }
        } catch (Exception e) { // catch on new ServerSocket.
            e.printStackTrace();
        }
    }

    /**
     * Server read and identify peripheral message received from the socket.
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
            eMessageType receivedMessageType = eMessageType.valueOf(received[0]);
            String receivedMessageContent = received[1];

            /* Answer to either Clients or the LFD. */
            answerToPeripheral(receivedMessageType, receivedMessageContent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Respond to the LFD / server / client.
     * @param receivedMessageType
     * @param receivedMessageContent
     */
    private void answerToPeripheral(eMessageType receivedMessageType, String receivedMessageContent) {
        String serverReplyMessage = "";
        try (Socket serverSocket = m_ServerSocket.accept()){
            OutputStream out = serverSocket.getOutputStream();
            if (receivedMessageType == eMessageType.LFD_HEART_BEAT) {
                m_Logger.log(Level.INFO, String.format("S%d received Heart Beat from LFD%d: %s", id, id, receivedMessageContent));
                serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.HEART_BEAT_ANSWER.name(), id, Server.END_CHAR);
                m_Logger.log(Level.INFO, String.format("Heart Beat Answer sent to LFD%d from S%d: %s\n", id, id, serverReplyMessage));
            } else if (receivedMessageType == eMessageType.CLIENT1_DATA) {
                /* Change server state to RED. */
                m_eServerState = eServerState.RED;
                m_Logger.log(Level.INFO, String.format("S%d received message from C1: %s", id, receivedMessageContent));
                m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.MESSAGE_ANSWER.name(), id, Server.END_CHAR);
                m_Logger.log(Level.INFO, String.format("Message reply sent to C1 from S%d: %s\n", id, serverReplyMessage));
            } else if (receivedMessageType == eMessageType.CLIENT2_DATA) {
                /* Change server state to YELLOW. */
                m_eServerState = eServerState.YELLOW;
                m_Logger.log(Level.INFO, String.format("S%d received message from C2: %s", id, receivedMessageContent));
                m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.MESSAGE_ANSWER.name(), id, Server.END_CHAR);
                m_Logger.log(Level.INFO, String.format("Message reply sent to C2 from S%d: %s\n", id, serverReplyMessage));
            } else if (receivedMessageType == eMessageType.CLIENT3_DATA) {
                /* Change server state to BLUE. */
                m_eServerState = eServerState.BLUE;
                m_Logger.log(Level.INFO, String.format("S%d received message from C3: %s", id, receivedMessageContent));
                m_Logger.log(Level.INFO, String.format("S%d current state: %s", id, m_eServerState.name()));
                serverReplyMessage = String.format("Server%d_%s %d%c", id, eMessageType.HEART_BEAT_ANSWER.name(), id, Server.END_CHAR); ;
                m_Logger.log(Level.INFO, String.format("Message reply sent to C3 from S%d: %s\n", id, serverReplyMessage));
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
        int id = Integer.parseInt(args[0]);
        System.out.printf("Server replica S1 has been launched at %s:%d\n\n", String.format(myIP, id), myPortNumber * 10 + id);
        Server m_server = new Server(myIP, myPortNumber, id);
        m_server.startService();
    }

}
