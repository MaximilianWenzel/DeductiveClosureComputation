package networking;

public class ServerData {

    private String serverName;
    private int portNumber;

    public ServerData(String serverName, int portNumber) {
        this.serverName = serverName;
        this.portNumber = portNumber;
    }

    public String getServerName() {
        return serverName;
    }

    public int getPortNumber() {
        return portNumber;
    }
}
