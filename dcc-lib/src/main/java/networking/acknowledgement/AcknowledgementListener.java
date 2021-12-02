package networking.acknowledgement;

public interface AcknowledgementListener {

    void onMessageAcknowledged(long messageID);
}
