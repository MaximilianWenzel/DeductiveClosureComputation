package reasoning.saturator;

import reasoning.rules.Rule;

import java.util.Collection;

public class PartitionModel {

    protected Collection<? extends Rule> rules;

    public PartitionModel(Collection<? extends Rule> rules) {
        this.rules = rules;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }

}
