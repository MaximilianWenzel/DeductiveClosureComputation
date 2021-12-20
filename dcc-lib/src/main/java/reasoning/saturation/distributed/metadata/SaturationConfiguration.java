package reasoning.saturation.distributed.metadata;

import java.io.Serializable;

public class SaturationConfiguration implements Serializable {

    private boolean collectingStatistics = false;

    public SaturationConfiguration() {
    }

    public SaturationConfiguration(boolean collectingStatistics) {
        this.collectingStatistics = collectingStatistics;
    }

    public boolean collectStatistics() {
        return collectingStatistics;
    }
}
