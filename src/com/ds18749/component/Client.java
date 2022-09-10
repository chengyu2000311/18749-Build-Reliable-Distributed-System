package com.ds18749.component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.*;


public class Client{
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;
    public static int clientStateCounter = 0;

    private final int clientId; 
    private Logger m_Logger;
    private int counter = 0;

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
            String messageString = "CLIENT" + clientId + "_DATA" + " " + counter + Server.END_CHAR;
            counter+=1;

            /* Open socket and write to server. */
            try (Socket client = new Socket(serverIP, serverPortNumber)){
                OutputStream out = client.getOutputStream();
                out.write(messageString.getBytes());
            } catch (Exception e){
                e.printStackTrace();
            }

            /* Listen for response from the server. */
            listenToMessageReponseFromServer();

            /* Log to console on the heartbeat message sent. */
            m_Logger.log(Level.INFO, String.format("Client%d sent out message to Server1: %s", clientId, messageString));

            /* Send message every 1 second. */
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Listen to server's reponse to the message we (the client) send.
     */
    private boolean listenToMessageReponseFromServer() {
        StringBuilder rawStringReceived = new StringBuilder();

        // Accept the socket and print received message from clients.
        try {
            Socket clientSocket_= new Socket(serverIP, serverPortNumber);
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
                m_Logger.log(Level.INFO, String.format("Received message answered by Server1: %s\n", receivedMessage));
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Check if current message is the server's reponse to our (the client's) message.
     * @param serverMessage
     * @return
     */
    private boolean isMessageRespondFromServer(String serverMessage) {
        return serverMessage.equals(Server.eMessageType.SERVER1_MESSAGE_ANSWER.name());
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
        m_client.startSendingMessageToServer();
    }
}
