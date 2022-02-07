package reasoning.saturation.distributed.metadata;

import java.io.Serializable;

public class SaturationConfiguration implements Serializable {

    protected boolean collectControlNodeStatistics = false;
    protected boolean collectWorkerNodeStatistics = false;

    public SaturationConfiguration() {
    }

    public SaturationConfiguration(boolean collectControlNodeStatistics,
                                   boolean collectWorkerNodeStatistics) {
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
