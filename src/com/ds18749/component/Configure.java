package com.ds18749.component;

public class Configure {
    private Server server;
    private Client[] clients = new Client[3];
    public static String serverIP = "127.0.0.1";
    public static int serverPortNumber = 4321;

    public Configure() {
        this.server = new Server(serverIP, serverPortNumber);
        for (int i=0; i<3; ++i) {
            this.clients[i] = new Client(i+1);
        }
    }

    public void start() {
        Thread serverThread = new Thread(server);
        serverThread.start();
        for (int i=0; i<3; ++i) {
            Thread clientThread = new Thread(clients[i]);
            clientThread.start();
        }
    }
}
