package com.ds18749.component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicationManager {
    public static final String myIP = "ece003.ece.local.cmu.edu";
//    public static final String myIP = "0.0.0.0";

    public static final int myPortNumber = 4412;

    private final List<String> memberships;

    private ServerSocket m_ServerSocket;

    private final Logger m_Logger;

    public ReplicationManager(){

        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        this.memberships = new ArrayList<>();
//        this.member_count = member_count;
//        this.start();
    }

    private void listenMemberMessage(){
        while (true){
            StringBuilder rawStringReceived = new StringBuilder();

            try(Socket serverSocket = this.m_ServerSocket.accept()){
                InputStream in = serverSocket.getInputStream();
                for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                    rawStringReceived.append((char) c);
                }
                String received = rawStringReceived.toString();
                updateMembership(received);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void updateMembership(String members){
//        List<String> new_members = new ArrayList<>();
        memberships.clear();
        for(int i = 0; i < members.length(); i++){
            String str = "S" + members.charAt(i);
            memberships.add(str);
        }
        m_Logger.log(Level.INFO, String.format("RM: %d members %s", memberships.size(), String.join(" ", memberships)));
    }

    private void start(){
        m_Logger.log(Level.INFO, "RM: 0 members");
        try {
            InetAddress serverAddress = InetAddress.getByName(myIP);
            this.m_ServerSocket = new ServerSocket(myPortNumber, 10, serverAddress);

            Thread RMThread = new Thread(this::listenMemberMessage);
            RMThread.start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ReplicationManager RM = new ReplicationManager();
        RM.start();
        System.out.printf("RM has been launched at %s:%d\n", myIP, myPortNumber);
    }
}
