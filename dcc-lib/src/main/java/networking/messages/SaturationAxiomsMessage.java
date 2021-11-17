package networking.messages;

import java.util.List;

public class SaturationAxiomsMessage<P> implements MessageModel {

    private List<P> axioms;

    public SaturationAxiomsMessage(List<P> axioms) {
        this.axioms = axioms;
    }

    public List<P> getAxioms() {
        return axioms;
    }

    @Override
    public int getMessageID() {
        return MessageType.SATURATION_DATA;
    }
}
