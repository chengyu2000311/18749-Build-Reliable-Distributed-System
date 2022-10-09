package util;

public class IpPort {
    private String IPAddress;
    private int portNumber;

    public IpPort(String IPAddress, int portNumber) {
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }

    public String IPAddress() {
        return IPAddress;
    }

    public int portNumber() {
        return portNumber;
    }
}
