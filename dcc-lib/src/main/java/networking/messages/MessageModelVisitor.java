package networking.messages;

public interface MessageModelVisitor {
    void visit(InitializePartitionMessage message);
    void visit(StateInfoMessage message);
    void visit(SaturationAxiomsMessage message);
    void visit(DebugMessage message);
}
