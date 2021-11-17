package networking.messages;

import data.Dataset;
import reasoning.rules.Rule;

import java.util.Collection;

public class InitPartitionMessage<P, T> implements MessageModel {

    private Collection<? extends Rule<P>> rules;
    private Dataset<P, T> datasetFragment;

    public InitPartitionMessage(Collection<? extends Rule<P>> rules, Dataset<P, T> datasetFragment) {
        this.rules = rules;
        this.datasetFragment = datasetFragment;
    }

    @Override
    public int getMessageID() {
        return MessageType.INITIALIZE_PARTITION;
    }

    public Collection<? extends Rule<P>> getRules() {
        return rules;
    }

    public Dataset<P, T> getDatasetFragment() {
        return datasetFragment;
    }
}
