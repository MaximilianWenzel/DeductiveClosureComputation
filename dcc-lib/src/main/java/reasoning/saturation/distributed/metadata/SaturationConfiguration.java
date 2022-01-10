package reasoning.saturation.distributed.metadata;

import java.io.Serializable;

public class SaturationConfiguration implements Serializable {

    private boolean collectControlNodeStatistics = false;
    private boolean collectWorkerNodeStatistics = false;

    public SaturationConfiguration() {
    }

    public SaturationConfiguration(boolean collectControlNodeStatistics, boolean collectWorkerNodeStatistics) {
        this.collectControlNodeStatistics = collectControlNodeStatistics;
        this.collectWorkerNodeStatistics = collectWorkerNodeStatistics;
    }

    public boolean collectControlNodeStatistics() {
        return collectControlNodeStatistics;
    }

    public boolean collectWorkerNodeStatistics() {
        return collectWorkerNodeStatistics;
    }
}
