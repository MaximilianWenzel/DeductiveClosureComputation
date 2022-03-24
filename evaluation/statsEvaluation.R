library(ggplot2)
library("scales")
library("gridExtra")
library(ggpubr)
library(tidyverse)
library(scales)
options(scipen=10000000)



#approach <- "distributed_multithreaded"
#approach <- "distributed_separate_jvm"

bTreeCSVFile <- paste("BinaryTree_benchmark.csv", sep="")
echoCSVFile <- paste("Echo_benchmark.csv", sep="")

bTreeCSVPath <- paste(getwd(), bTreeCSVFile, sep="/")
echoCSVPath <- paste(getwd(), echoCSVFile, sep="/")

df <- read.table(bTreeCSVPath, 
                 header = TRUE,
                 sep = ";")

df <- rbind(df, read.table(echoCSVPath, 
                 header = TRUE,
                 sep = ";"))




# binary tree depth 17-20, workers 1-12
bTreePlots <- lapply(17:20, function(i) {
  data = subset(df, 
                #df$approach!="single" &
                df$numWorkers<=8 &
                df$benchmarkType=="BinaryTree" &
                  df$numberOfInitialAxioms== 2^i - 2 &
                  df$messageDistribution=="add_own_messages_directly_to_todo" &
                  (df$withSendingClosureResultsTime=="false" | df$approach=="single")
  )
  p = ggplot(data = data,
             aes(x=data$numWorkers,
                 y=data$averageRuntimeMS,
                 color=data$approach)
             ) +
    geom_point(stat="identity", size=1.5) +
    geom_line() +
    labs(title=paste("B-Tree Depth: ", i, sep=""),
         x="Number of Workers",
         y="Average Runtime [ms]",
         color="Approach") +
    theme_bw() +
    scale_x_continuous(breaks = pretty_breaks()) +
    scale_y_continuous(labels = scales::comma)
  return(p)
})

outputFileName = paste("binaryTreeResults.pdf", sep="")
pdf(file=outputFileName,
    width=7,
    height=10,
    onefile = FALSE
)
ggarrange(bTreePlots[[1]],
          bTreePlots[[2]],
          bTreePlots[[3]],
          bTreePlots[[4]],
             nrow = 4,
             common.legend = TRUE,
             legend ="right"
)     
dev.off()


# b-tree speedup plot
speedupBTreePlots <- lapply(17:20, function(i) {

  data = subset(df, 
                #df$approach!="single" &
                df$numWorkers<=8 &
                df$benchmarkType=="BinaryTree" &
                  df$numberOfInitialAxioms== 2^i - 2 &
                  df$messageDistribution=="add_own_messages_directly_to_todo" &
                  (df$withSendingClosureResultsTime=="false" | df$approach=="single")
  )
  singleData = subset(data, data$approach=="single")
  singleAVGRuntime = singleData$averageRuntimeMS[[1]]
  data$averageRuntimeMS = singleAVGRuntime / data$averageRuntimeMS
  p = ggplot(data = data,
             aes(x=data$numWorkers,
                 y=data$averageRuntimeMS,
                 color=data$approach)
  ) +
    geom_point(stat="identity", size=1.5) +
    geom_line() +
    labs(title=paste("B-Tree Depth: ", i, sep=""),
         x="Number of Workers",
         y="Speedup",
         color="Approach") +
    theme_bw() +
    scale_x_continuous(breaks = pretty_breaks()) +
    scale_y_continuous(labels = scales::comma)
  return(p)
})

outputFileName = paste("binaryTreeSpeedup.pdf", sep="")
pdf(file=outputFileName,
    width=7,
    height=10,
    onefile = FALSE
)
ggarrange(speedupBTreePlots[[1]],
          speedupBTreePlots[[2]],
          speedupBTreePlots[[3]],
          speedupBTreePlots[[4]],
          nrow = 4,
          common.legend = TRUE,
          legend ="right"
)     
dev.off()

# binary tree: send all messages over network vs. add to own to-do
bTreeMessagesBenchmark <- lapply(17:20, function(i) {
  
  data = subset(df, 
                #df$approach!="single" &
                df$numWorkers<=8 &
                df$benchmarkType=="BinaryTree" &
                  df$numberOfInitialAxioms==2^i - 2 &
                  df$withSendingClosureResultsTime=="false" &
                  df$approach=="distributed_separate_jvm"
  )
  
  p = ggplot(data = data,
             aes(x=data$numWorkers,
                 y=data$averageRuntimeMS,
                 color=data$messageDistribution)
  ) +
    geom_point(stat="identity", size=1.5) +
    geom_line() +
    labs(title=paste("Distributed, Separate JVM, B-Tree Depth: ", i, sep=""),
         x="Number of Workers",
         y="Average Runtime [ms]",
         color="Approach") +
    theme_bw() +
    scale_x_continuous(breaks = pretty_breaks()) +
    scale_y_continuous(labels = scales::comma)
  return(p)
})

outputFileName = paste("binaryTreeMessagesBenchmark.pdf", sep="")
pdf(file=outputFileName,
    width=7,
    height=2.5,
    onefile = FALSE
)
ggarrange(bTreeMessagesBenchmark[[4]],
          nrow = 1,
          common.legend = TRUE,
          legend ="right"
)     
dev.off()



# echo benchmark, workers 1-8
echoAxioms <- list(5e+5, 1e+6, 5e+6, 1e+7)
echoPlots <- lapply(echoAxioms, function(i) {
  data = subset(df, 
                #df$approach!="single" &
                df$numWorkers<=8 &
                df$benchmarkType=="Echo" &
                  df$numberOfInitialAxioms==i &
                  df$messageDistribution=="add_own_messages_directly_to_todo" &
                  (df$withSendingClosureResultsTime=="false" | df$approach=="single")
  )
  p = ggplot(data = data,
             aes(x=data$numWorkers,
                 y=data$averageRuntimeMS,
                 color=data$approach)
  ) +
    geom_point(stat="identity", size=1.5) +
    geom_line() +
    labs(title=paste("Echo Axioms: ", prettyNum(i,big.mark=",",scientific=FALSE), sep=""),
         x="Number of Workers",
         y="Average Runtime [ms]",
         color="Approach") +
    theme_bw() +
    scale_x_continuous(breaks = pretty_breaks()) +
    scale_y_continuous(labels = scales::comma)
  return(p)
})

outputFileName = paste("echoResults.pdf", sep="")
pdf(file=outputFileName,
    width=7,
    height=10,
    onefile = FALSE
)
ggarrange(echoPlots[[1]],
          echoPlots[[2]],
          echoPlots[[3]],
          echoPlots[[4]],
          nrow = 4,
          common.legend = TRUE,
          legend ="right"
)     
dev.off()


# echo speedup plot
speedupEchoPlots <- lapply(echoAxioms, function(i) {
  
  data = subset(df, 
                df$numWorkers<=8 &
                df$benchmarkType=="Echo" &
                  df$numberOfInitialAxioms== i &
                  df$messageDistribution=="add_own_messages_directly_to_todo" &
                  (df$withSendingClosureResultsTime=="false" | df$approach=="single")
  )
  singleData = subset(data, data$approach=="single")
  singleAVGRuntime = singleData$averageRuntimeMS[[1]]
  data$averageRuntimeMS = singleAVGRuntime / data$averageRuntimeMS
  p = ggplot(data = data,
             aes(x=data$numWorkers,
                 y=data$averageRuntimeMS,
                 color=data$approach)
  ) +
    geom_point(stat="identity", size=1.5) +
    geom_line() +
    labs(title=paste("Echo Axioms: ", prettyNum(i,big.mark=",",scientific=FALSE), sep=""),
         x="Number of Workers",
         y="Speedup",
         color="Approach") +
    theme_bw() +
    scale_x_continuous(breaks = pretty_breaks()) +
    scale_y_continuous(labels = scales::comma)
return(p)
  return(p)
})

outputFileName = paste("echoSpeedup.pdf", sep="")
pdf(file=outputFileName,
    width=7,
    height=10,
    onefile = FALSE
)
ggarrange(speedupEchoPlots[[1]],
          speedupEchoPlots[[2]],
          speedupEchoPlots[[3]],
          speedupEchoPlots[[4]],
          nrow = 4,
          common.legend = TRUE,
          legend ="right"
)     
dev.off()
