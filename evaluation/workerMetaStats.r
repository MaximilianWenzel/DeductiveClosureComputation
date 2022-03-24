library(ggplot2)
library("scales")
library("gridExtra")
library(ggpubr)
library(tidyverse)
library(scales)
options(scipen=10000000)

titleSize <- 11


# control node statistics
controlNodeStatsCSV <- paste(getwd(), "controlNodeMetaStats.csv", sep="/")

cnStats <- read.table(controlNodeStatsCSV, 
                 header = TRUE,
                 sep = ";")


# worker statistics
workerStatsCSV <- paste(getwd(), "workerMetaStats.csv", sep="/")
workerStats <- read.table(workerStatsCSV, 
                          header = TRUE,
                          sep = ";")



benchmark = "BinaryTree"

workerData = subset(workerStats, 
              #df$approach!="single" &
              workerStats$numWorkers<=8 &
                workerStats$benchmarkType==benchmark &
                workerStats$messageDistribution=="add_own_messages_directly_to_todo" 
)

cnData = subset(cnStats, 
                cnStats$numWorkers<=8 &
                  cnStats$benchmarkType==benchmark &
                  cnStats$messageDistribution=="add_own_messages_directly_to_todo" 
)

# time applying rules
ruleTimePlot <- ggplot(data=workerData,
                       aes(x=workerData$numWorkers,
                           y=workerData$AVGApplyingRulesTimeWhileSaturationMS,
                           fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Applying Rules",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


distributingMessages <- ggplot(data = workerData,
                               aes(x=workerData$numWorkers,
                                   y=workerData$AVGDistributingAxiomsTimeMS,
                                   fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Distributing Axioms",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


waitingTime <- ggplot(data = workerData,
                      aes(x=workerData$numWorkers,
                          y=workerData$AVGWorkerWaitingTimeWhileSaturationMS,
                          fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Waiting Time per Worker ",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


initializeConnections <- ggplot(data = workerData,
                                aes(x=workerData$numWorkers,
                                    y=workerData$AVGWorkerInitializingConnectionsToOtherWorkersMS,
                                    fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Initializing Connections to Other Workers",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


todoIsEmptyEvents <- ggplot(data = workerData,
                            aes(x=workerData$numWorkers,
                                y=workerData$AVGToDoIsEmptyEvent,
                                fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Number of 'To-Do is Empty' Events",
       x="Number of Workers",
       y="# Events",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


processedAxioms <- ggplot(data = workerData,
                          aes(x=workerData$numWorkers,
                              y=workerData$AVGNumberOfProcessedAxioms,
                              fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Processed Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))

derivedAxioms <- ggplot(data = workerData,
                        aes(x=workerData$numWorkers,
                            y=workerData$AVGNumberOfDerivedInferences,
                            fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Derived Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


sentAxioms <- ggplot(data = workerData,
                     aes(x=workerData$numWorkers,
                         y=workerData$AVGNumberOfSentAxioms,
                         fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Sent Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


receivedAxioms <- ggplot(data = workerData,
                         aes(x=workerData$numWorkers,
                             y=workerData$AVGNumberOfReceivedAxioms,
                             fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Received Axioms",
       x="Number of Workers",
       y="# Events",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))

saturationTime <- ggplot(data = cnData,
                                 aes(x=cnData$numWorkers,
                                     y=cnData$TotalSaturationTimeMS,
                                     fill=cnData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Time Until All Workers Found Fixpoint",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


sendingClosureResultsTime <- ggplot(data = cnData,
                                    aes(x=cnData$numWorkers,
                                        y=cnData$CollectingClosureResultsTimeMS,
                                        fill=cnData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Control Node Collecting Closure Results",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))



# generate plot
outputFileName = paste(benchmark, "_workerMetaTimes.pdf", sep="")
pdf(file=outputFileName,
    width=6,
    height=10,
    onefile = FALSE
)
p <- ggarrange(initializeConnections,
          ruleTimePlot,
          distributingMessages,
          waitingTime,
          saturationTime,
          sendingClosureResultsTime,
          nrow = 6,
          common.legend = TRUE,
          legend ="right"
)     
#p <- annotate_figure(p, top = text_grob("Binary Tree Depth 19"), size = 14)
print(p)
dev.off()


outputFileName = paste(benchmark, "_workerAxiomStats.pdf", sep="")
pdf(file=outputFileName,
    width=6,
    height=10,
    onefile = FALSE
)
p <- ggarrange(receivedAxioms,
          processedAxioms,
          derivedAxioms,
          sentAxioms,
          todoIsEmptyEvents,
          nrow = 5,
          common.legend = TRUE,
          legend ="right"
)
p <- annotate_figure(p, top = text_grob("Binary Tree Depth 19"), size = 14)
print(p)
dev.off()


###############################################################################
benchmark = "Echo"
workerData = subset(workerStats, 
              #df$approach!="single" &
              workerStats$numWorkers<=8 &
                workerStats$benchmarkType==benchmark &
                workerStats$messageDistribution=="add_own_messages_directly_to_todo" 
)

cnData = subset(cnStats, 
                cnStats$numWorkers<=8 &
                  cnStats$benchmarkType==benchmark &
                  cnStats$messageDistribution=="add_own_messages_directly_to_todo" 
)

# time applying rules
ruleTimePlot <- ggplot(data = workerData,
                       aes(x=workerData$numWorkers,
                           y=workerData$AVGApplyingRulesTimeWhileSaturationMS,
                           fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Applying Rules",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))



distributingMessages <- ggplot(data = workerData,
                               aes(x=workerData$numWorkers,
                                   y=workerData$AVGDistributingAxiomsTimeMS,
                                   fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Distributing Axioms",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


waitingTime <- ggplot(data = workerData,
                      aes(x=workerData$numWorkers,
                          y=workerData$AVGWorkerWaitingTimeWhileSaturationMS,
                          fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Waiting Time per Worker ",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


initializeConnections <- ggplot(data = workerData,
                                aes(x=workerData$numWorkers,
                                    y=workerData$AVGWorkerInitializingConnectionsToOtherWorkersMS,
                                    fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Time per Worker Initializing Connections to Other Workers",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


todoIsEmptyEvents <- ggplot(data = workerData,
                            aes(x=workerData$numWorkers,
                                y=workerData$AVGToDoIsEmptyEvent,
                                fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Average Number of 'To-Do is Empty' Events",
       x="Number of Workers",
       y="# Events",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


processedAxioms <- ggplot(data = workerData,
                          aes(x=workerData$numWorkers,
                              y=workerData$AVGNumberOfProcessedAxioms,
                              fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Processed Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))

derivedAxioms <- ggplot(data = workerData,
                        aes(x=workerData$numWorkers,
                            y=workerData$AVGNumberOfDerivedInferences,
                            fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Derived Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


sentAxioms <- ggplot(data = workerData,
                     aes(x=workerData$numWorkers,
                         y=workerData$AVGNumberOfSentAxioms,
                         fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Sent Axioms",
       x="Number of Workers",
       y="# Axioms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


receivedAxioms <- ggplot(data = workerData,
                         aes(x=workerData$numWorkers,
                             y=workerData$AVGNumberOfReceivedAxioms,
                             fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Received Axioms",
       x="Number of Workers",
       y="# Events",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))



requiredSaturationTime <- ggplot(data = cnData,
                                 aes(x=cnData$numWorkers,
                                     y=cnData$TotalSaturationTimeMS,
                                     fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Time Until All Workers Found Fixpoint",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


sendingClosureResultsTime <- ggplot(data = cnData,
                                 aes(x=cnData$numWorkers,
                                     y=cnData$CollectingClosureResultsTimeMS,
                                     fill=workerData$approach)
) +
  geom_bar(stat="identity", position='dodge') +
  labs(title="Control Node Collecting Closure Results",
       x="Number of Workers",
       y="ms",
       fill="Approach") +
  theme_minimal() +
  scale_x_continuous(breaks = c(1, 4, 8)) +
  scale_y_continuous(labels = scales::comma) +
  theme(plot.title=element_text(size=titleSize))


# generate plot
outputFileName = paste(benchmark, "_workerMetaTimes.pdf", sep="")
pdf(file=outputFileName,
    width=6,
    height=10,
    onefile = FALSE
)
p <- ggarrange(initializeConnections,
               ruleTimePlot,
               distributingMessages,
               waitingTime,
               saturationTime,
               sendingClosureResultsTime,
               nrow = 6,
               common.legend = TRUE,
               legend ="right"
)   
p <- annotate_figure(p, top = text_grob("Echo Benchmark, 5M Initial Axioms"), size = 14)
print(p)
dev.off()


outputFileName = paste(benchmark, "_workerAxiomStats.pdf", sep="")
pdf(file=outputFileName,
    width=6,
    height=10,
    onefile = FALSE
)

p <- ggarrange(receivedAxioms,
          processedAxioms,
          derivedAxioms,
          sentAxioms,
          todoIsEmptyEvents,
          nrow = 5,
          common.legend = TRUE,
          legend ="right"
)
p <- annotate_figure(p, top = text_grob("Echo Benchmark, 5M Initial Axioms"), size = 14)
print(p)

dev.off()












