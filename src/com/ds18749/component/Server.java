package com.ds18749.component;

import java.io.InputStream;
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

    public Server(String IPAddress, int portNumber) {
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }

    public void startService() {
        try {
            InetAddress serverAddress = InetAddress.getByName(IPAddress);
            try(ServerSocket service = new ServerSocket(portNumber, 10, serverAddress)){
                while (true) {
                    StringBuilder receivedMessage = new StringBuilder();

                    // Accept the socket and print received message from clients.
                    try(Socket connect = service.accept()){
                        InputStream in = connect.getInputStream();

                        // Store socket instream.
                        for (int c = in.read(); c != END_CHAR; c = in.read()) {
                            if(c ==-1)
                                break;
                            receivedMessage.append((char) c);
                        }
                        // System.out.printf("Received: %s", receivedMessage);

                        String receivedRaw = receivedMessage.toString();
                        System.out.printf("Received: %s\n", receivedRaw);

                        /*
                        String[] received = receivedMessage.toString().split(" ", 2);
                        String sign = received[0], id = received[1];
                        int idNum = Integer.parseInt(id);

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
            } catch (Exception e){ // catch on service.accept()
                e.printStackTrace();
            }
        } catch (UnknownHostException e) { // catch on new serversockect.
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        System.out.printf("Server replica S1 has been launched at %s:%d\n", serverIP, serverPortNumber);
        Server m_server = new Server(serverIP, serverPortNumber);
        m_server.startService();
    }
}
