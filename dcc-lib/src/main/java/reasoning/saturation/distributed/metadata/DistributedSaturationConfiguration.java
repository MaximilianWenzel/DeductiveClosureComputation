package reasoning.saturation.distributed.metadata;

import enums.MessageDistributionType;

public class DistributedSaturationConfiguration extends SaturationConfiguration {

    private MessageDistributionType messageDistributionType;

    DistributedSaturationConfiguration() {

    }

    public DistributedSaturationConfiguration(boolean collectControlNodeStatistics, boolean collectWorkerNodeStatistics,
                                              MessageDistributionType messageDistributionType) {
        super(collectControlNodeStatistics, collectWorkerNodeStatistics);
        this.messageDistributionType = messageDistributionType;
    }

    public MessageDistributionType getMessageDistributionType() {
        return messageDistributionType;
    }
}
