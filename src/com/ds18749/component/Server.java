package com.ds18749.component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Server implements Runnable {
    public static final char END_CHAR = '#';
    private final String IPAddress;
    private final int portNumber;
    private Map<Integer, Integer> record = new HashMap<>();

    public Server(String IPAddress, int portNumber) {
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }

    public void startService() {
        try {
            InetAddress serverAddress = InetAddress.getByName(IPAddress);
            try(ServerSocket service = new ServerSocket(portNumber, 10, serverAddress)){
                while (true) {
                    StringBuilder receiveMsg = new StringBuilder();
                    try(Socket connect = service.accept()){
                        InputStream in = connect.getInputStream();

                        for (int c = in.read(); c != END_CHAR; c = in.read()) {
                            if(c ==-1)
                                break;
                            receiveMsg.append((char) c);
                        }
                        String[] received = receiveMsg.toString().split(" ", 2);
                        String sign = received[0], id = received[1];
                        int idNum = Integer.parseInt(id);
                        if (sign.equals("plus")) {
                            record.put(idNum, record.getOrDefault(idNum, 0) + 1);
                        } else {
                            record.put(idNum, record.getOrDefault(idNum, 0) - 1);
                        }
                        System.out.printf("Get message from client%d, and current record is %s%n",
                                idNum, record.toString());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        startService();
    }
}
