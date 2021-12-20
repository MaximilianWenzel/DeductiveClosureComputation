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

    public String getServerName() {
        return serverName;
    }

    public int getPortNumber() {
        return portNumber;
    }
}
