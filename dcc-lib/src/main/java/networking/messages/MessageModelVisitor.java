package networking.messages;

public interface MessageModelVisitor {
    void visit(InitializeWorkerMessage message);
    void visit(StateInfoMessage message);
    void visit(SaturationAxiomsMessage message);
    void visit(DebugMessage message);
    void visit(AcknowledgementMessage message);
}
