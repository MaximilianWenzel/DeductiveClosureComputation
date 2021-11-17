package networking;

import java.io.Serializable;

public class SerializedObjectTest implements Serializable {
    private String str;

    public SerializedObjectTest(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }
}
