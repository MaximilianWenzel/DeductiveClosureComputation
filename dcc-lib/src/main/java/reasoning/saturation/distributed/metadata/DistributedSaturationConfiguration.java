package reasoning.saturation.distributed.metadata;

import enums.MessageDistributionType;

/**
 * This class contains the configuration for computing the deductive closure in a distributed setup, e.g., in order to determine whether all
 * messages should be transmitted over the network or messages destined to the same node are added directly to the to-do queue again.
 */
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
