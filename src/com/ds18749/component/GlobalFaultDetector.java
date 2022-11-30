package com.ds18749.component;

import util.IpPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalFaultDetector {
    public static final String myIP = "ece003.ece.local.cmu.edu";
    public static final int myPortNumber = 4311;

    private ServerSocket m_ServerSocket;
    private final long heartBeatFrequency;
    private final Logger m_Logger;
    private final Lock lastHearBeatLock = new ReentrantLock();
    private final Map<Integer, Timestamp> lastHearBeat = new HashMap<>();
    private final long timeout = 4 * 1000L; // in millisecond

    private final List<String> memberships;

    enum eMessageType {
        GDB_HEART_BEAT,
        GDB_HEART_BEAT_ANSWER,
    }

    private final List<IpPort> LFDIpPortS = Arrays.asList(
            new IpPort("ece000.ece.local.cmu.edu", 4321),
            new IpPort("ece001.ece.local.cmu.edu", 4322),
            new IpPort("ece002.ece.local.cmu.edu", 4323)
    );

    public GlobalFaultDetector(long heartBeatFrequency) {
        this.heartBeatFrequency = heartBeatFrequency;
        this.m_Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        this.memberships = new ArrayList<>();
        this.start();
    }

    /**
     * Check if timeout occurs
     */
    private void checkTimeout() {
        while (true) {
            lastHearBeatLock.lock();
            List<Integer> remove = new ArrayList<>();
            for (Map.Entry<Integer, Timestamp> entry : lastHearBeat.entrySet()) {
                Integer LfdId = entry.getKey();
                Timestamp timeoutSecond = new Timestamp(System.currentTimeMillis() - timeout);
                Timestamp last = entry.getValue();
                if (timeoutSecond.after(last)) {
                    remove.add(LfdId);
                    m_Logger.log(Level.INFO, String.format("TIMEOUT. GDF does not hear Heart Beat Answer from the LFD%d.", LfdId));
                }
            }
            for (int LfdId: remove) {
                lastHearBeat.remove(LfdId);
            }
            lastHearBeatLock.unlock();
        }
    }

    /**
     * continuing listen to message from other LFD
     */
    private void listenMessage() {
        while (true) {
            StringBuilder rawStringReceived = new StringBuilder();

            /* Accept the socket and print received message from clients. */
            try(Socket serverSocket = this.m_ServerSocket.accept()) {
                InputStream in = serverSocket.getInputStream();

                /* Record socket instream. */
                for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                    rawStringReceived.append((char) c);
                }

                /* Parse the message received. */
                String received = rawStringReceived.toString();
                /* Answer to either Clients or the LFD. */
                System.out.println(received);
                updateState(received);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * @param received
     * update membership based on what is received
     */
    private void updateState(String received) {
        System.out.println("Start updating");
        int id = Integer.parseInt(String.valueOf(received.charAt(received.length()-1)));
        if (received.contains("add replica")) {
            memberships.add("S" + id);
            System.out.println(memberships);
            m_Logger.log(Level.INFO, String.format("add GDF: %d members %s", memberships.size(), String.join(" ", memberships)));
        } else if (received.contains("delete replica")) {
            memberships.remove("S" + id);
            System.out.println(memberships);
            m_Logger.log(Level.INFO, String.format("delete GDF: %d members %s", memberships.size(), String.join(" ", memberships)));
        }
        sendMemberListToRM(memberships);
    }
    /**
     * send the member list to RM
     */
    public void sendMemberListToRM(List<String> memberships){
        System.out.println("Start sending");
        StringBuilder sb = new StringBuilder();
        for(String membership : memberships) sb.append(membership.charAt(1));
        sb.append("#");
        String msg = sb.toString();
        m_Logger.log(Level.INFO, String.format("GFD send out membership message to RM: %s", msg));
        try {
            Socket m_globalFaultDetectorSocket = new Socket(ReplicationManager.myIP, ReplicationManager.myPortNumber);
            OutputStream out = m_globalFaultDetectorSocket.getOutputStream();
            out.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * start hearbeat with other LFD
     */
    private void start() {
        /* Setup timer for sending GFD heartbeat. */
        m_Logger.log(Level.INFO, "GFD: 0 members");
        try {
            InetAddress serverAddress = InetAddress.getByName(myIP);
            this.m_ServerSocket = new ServerSocket(myPortNumber, 10, serverAddress);

            Thread serverThread = new Thread(this::listenMessage);
            serverThread.start();

            Thread timeoutThread = new Thread(this::checkTimeout);
            timeoutThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(heartBeatFrequency * 1000L);
                sendHeartBeat();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * send heartbeats to all other LFDs
     */
    private void sendHeartBeat() {
        for (int id=1; id<4; ++id) {
            /* Example: GDB_HEART_BEAT */
            String msg = String.format("%s%c", eMessageType.GDB_HEART_BEAT, Server.END_CHAR);

            /* Open socket and write to the server. */
            try {
                IpPort ipPort = LFDIpPortS.get(id-1);
                Socket m_localFaultDetectorSocket = new Socket(ipPort.IPAddress(), ipPort.portNumber());
                OutputStream out = m_localFaultDetectorSocket.getOutputStream();
                out.write(msg.getBytes());
                /* Log to console on the heartbeat message sent. */
                m_Logger.log(Level.INFO, String.format("GDB send out heart beat message to LDF%d: %s", id, msg));

                StringBuilder rawStringReceived = new StringBuilder();
                m_localFaultDetectorSocket = new Socket(ipPort.IPAddress(), ipPort.portNumber());
                InputStream in = m_localFaultDetectorSocket.getInputStream();
                for (int c = in.read(); c != Server.END_CHAR; c = in.read()) {
                    if (c == -1) {
                        break;
                    }
                    rawStringReceived.append((char) c);
                }
                String receivedMessage = rawStringReceived.toString();
                if (receivedMessage.startsWith(eMessageType.GDB_HEART_BEAT_ANSWER.name())) {
                    m_Logger.log(Level.INFO, String.format("Heartbeat answered by from LFD%d: %s\n", id, receivedMessage));
                    lastHearBeatLock.lock();
                    this.lastHearBeat.put(id, new Timestamp(System.currentTimeMillis()));
                    lastHearBeatLock.unlock();
                }
            } catch (Exception e) {
                //    e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        GlobalFaultDetector GDF = new GlobalFaultDetector(2);
        GDF.start();
        System.out.printf("GlobalFault detector GFD has been launched at %s:%d\n", myIP, myPortNumber);
    }
}
