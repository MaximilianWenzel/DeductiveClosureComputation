package reasoning.saturator;

import data.Dataset;
import reasoning.rules.Rule;

import java.util.Collection;
import java.util.Set;

public class PartitionModel<P, T> {

    protected Collection<? extends Rule<P>> rules;
    protected Set<T> termPartition;
    protected Dataset<P, T> datasetFragment;

    public PartitionModel(Collection<? extends Rule<P>> rules,
                          Set<T> termPartition,
                          Dataset<P, T> datasetFragment) {
        this.rules = rules;
        this.termPartition = termPartition;
        this.datasetFragment = datasetFragment;
    }

    public Collection<? extends Rule<P>> getRules() {
        return rules;
    }

    public Set<T> getTermPartition() {
        return termPartition;
    }

    public Dataset<P, T> getDatasetFragment() {
        return datasetFragment;
    }
}
