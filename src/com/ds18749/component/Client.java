package com.ds18749.component;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class Client{
    private final String[] messages = new String[]{"plus", "minus"};
    private final int id;
    public static final String serverIP = "127.0.0.1";
    public static final int serverPortNumber = 4321;

    public Client(int id) {
        this.id = id;
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

    private String getRandomMsg() {
        Random r = new Random();
        int ind = r.nextInt(2) ;
        return messages[ind] + " " + id;
    }


    public void startToSend() {
        while (true) {
            String randomMsg = getRandomMsg();
            sendMsg(randomMsg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        Client m_client = new Client(id);
        m_client.startToSend();
    }
}
