package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.*;

public class Client{
    private final String[] statesString = new String[]{"plus", "minus"};
    private final int clientId; 
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;
    public static int clientStateCounter = 0;
    private Logger m_Logger;

    public Client(int id) {
        this.clientId = id;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    private void sendMsg(String msg) {
        msg = msg + Server.END_CHAR;
        try (Socket client = new Socket(serverIP, serverPortNumber)){
            OutputStream out = client.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startToSend() {
        while (true) {
            clientStateCounter += 1;
            String messageString = "CLIENT" + clientId + "_DATA" + " " + Server.END_CHAR;
            sendMsg(messageString);
            listenToHeartBeatAnswerFromServer();
            // Log to console on the heartbeat message sent.
            m_Logger.log(Level.INFO, String.format("Client%d sent out message to Server1: %s", clientId, messageString));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean listenToHeartBeatAnswerFromServer() {
        StringBuilder rawStringReceived = new StringBuilder();

        // Accept the socket and print received message from clients.
        try {
            Socket clientSocket_ = new Socket(serverIP, serverPortNumber);
            InputStream in = clientSocket_.getInputStream();

            // Store socket instream.
            for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                if(c ==-1)
                    break;
                rawStringReceived.append((char) c);
            }
            
            String receivedMessage = rawStringReceived.toString();
            String[] received = receivedMessage.toString().split(" ", 2);

            if (isMessageRespondFromServer(received[0])) {
                m_Logger.log(Level.INFO, String.format("Received message answered from Server1: %s\n", receivedMessage));
            } else {
                return false;
            }
        } catch (Exception e) {
            m_Logger.warning("LFD1 does not hear Heart Beat Answer from the Server1. Stop sending heart beats...\n");
            // e.printStackTrace();
        }
        return true;
    }

    private boolean isMessageRespondFromServer(String serverMessage) {
        return serverMessage.equals(Server.MessageType.SERVER1_MESSAGE_ANSWER.name());
    }


    public static void main(String[] args) {
        int clientId = Integer.parseInt(args[0]);
        Client m_client = new Client(clientId);
        System.out.printf("Client%d has been launched at %s:%d\n", clientId, serverIP, serverPortNumber);
        m_client.startToSend();
    }
}
