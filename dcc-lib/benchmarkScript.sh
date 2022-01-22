#!/bin/bash

NUM_NETWORKING_THREADS=1
PROJECT_NAME=dcc-lib

# docker benchmark
for NUM_WORKERS in 2 3 4 5 6 7 8 9 10 11 12 16 24
do
  for BENCHMARK in echo binarytree
  do
    APPROACH=docker-network NUM_WORKERS=$NUM_WORKERS BENCHMARK=$BENCHMARK NUM_NETWORKING_THREADS=$NUM_NETWORKING_THREADS \
    docker-compose -p $PROJECT_NAME -f DistributedSaturationBenchmark.yaml up \
    --abort-on-container-exit --exit-code-from control-node
  done
done

# JVM benchmark
for NUM_WORKERS in 2 3 4 5 6 7 8 9 10 11 12 16 24
do
  for BENCHMARK in echo binarytree
  do
    java -cp ./target/dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar benchmark.DockerSaturationBenchmark single-machine $NUM_WORKERS $BENCHMARK $NUM_NETWORKING_THREADS
  done
done



