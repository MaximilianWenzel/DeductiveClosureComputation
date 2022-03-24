#!/bin/bash
export UID=$(id -u)
export GID=$(id -g)

NUM_NETWORKING_THREADS=1
PROJECT_NAME=dcc-lib
COLLECT_WORKER_STATS=true

# docker benchmark
for NUM_WORKERS in 1 4 8 # 9 10 11 12
do
  for BENCHMARK in echo binarytree
  do
	APPROACH=docker-network NUM_WORKERS=$NUM_WORKERS BENCHMARK=$BENCHMARK NUM_NETWORKING_THREADS=$NUM_NETWORKING_THREADS \
	COLLECT_WORKER_STATS=$COLLECT_WORKER_STATS \
	UID=$UID GID=$GID \
	docker-compose -p $PROJECT_NAME -f DistributedSaturationBenchmark.yaml up \
	--abort-on-container-exit --exit-code-from control-node
  done
done

# JVM benchmark
for NUM_WORKERS in 1 4 8 # 9 10 11 12
do
  for BENCHMARK in echo binarytree
  do
	docker run \
	--user $UID:$GID \
	-v ~/:/saturation \
	saturation-control-node single-machine $NUM_WORKERS $BENCHMARK $COLLECT_WORKER_STATS
  done
done