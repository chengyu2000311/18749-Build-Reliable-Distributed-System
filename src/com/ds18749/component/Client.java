package com.ds18749.component;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class Client implements Runnable {
    private final String[] messages = new String[]{"plus", "minus"};
    private final int id;
    public Client(int id) {
        this.id = id;
    }
    private void sendMsg(String msg) {
        msg = msg+ Server.END_CHAR;
        try (Socket client = new Socket(Configure.serverIP, Configure.serverPortNumber)){
            OutputStream out = client.getOutputStream();
            out.write(msg.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getRandomMsg() {
        Random r = new Random();
        int ind = r.nextInt(2) ;
        return messages[ind] + " " + id;
    }


    public void startSend() {
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

    @Override
    public void run() {
        startSend();
    }
}
