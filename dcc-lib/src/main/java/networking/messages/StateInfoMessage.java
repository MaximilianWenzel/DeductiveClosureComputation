package networking.messages;

public class StateInfoMessage implements MessageModel {

    protected long stateSequenceNumber;
    protected int state;

    public StateInfoMessage(int stateSequenceNumber, int state) {
        this.stateSequenceNumber = stateSequenceNumber;
        this.state = state;
    }

    @Override
    public int getMessageID() {
        return MessageType.CONTROL_DATA;
    }

    public int getState() {
        return state;
    }

    public long getStateSequenceNumber() {
        return stateSequenceNumber;
    }
}
