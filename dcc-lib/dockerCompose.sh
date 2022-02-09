#!/bin/bash
export UID=$(id -u)
export GID=$(id -g)

NUM_NETWORKING_THREADS=1
PROJECT_NAME=dcc-lib


for APPROACH in docker-network
do
  for NUM_WORKERS in  2
  do
    for BENCHMARK in echo
    do
      APPROACH=$APPROACH NUM_WORKERS=$NUM_WORKERS BENCHMARK=$BENCHMARK \
	  NUM_NETWORKING_THREADS=$NUM_NETWORKING_THREADS \
	  UID=$UID GID=$GID \
      docker-compose -p $PROJECT_NAME -f DistributedSaturationBenchmark.yaml up \
      --abort-on-container-exit --exit-code-from control-node
    done
  done
done