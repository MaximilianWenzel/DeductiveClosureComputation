package networking;

import java.io.Serializable;

public class ServerData implements Serializable {

    private String serverName;
    private int portNumber;

    protected ServerData() {
    }

    public ServerData(String serverName, int portNumber) {
        this.serverName = serverName;
        this.portNumber = portNumber;
    }

    public String getHostname() {
        return serverName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public String toString() {
        return "ServerData{" +
                "serverName='" + serverName + '\'' +
                ", portNumber=" + portNumber +
                '}';
    }
}
