#!/bin/bash
export UID=$(id -u)
export GID=$(id -g)

NUM_NETWORKING_THREADS=1
PROJECT_NAME=dcc-lib
COLLECT_WORKER_STATS=false

# docker benchmark
for NUM_WORKERS in 1 2 3 4 5 6 7 8 # 9 10 11 12
do
  for BENCHMARK in binarytree echo 
  do
	APPROACH=docker-network NUM_WORKERS=$NUM_WORKERS BENCHMARK=$BENCHMARK NUM_NETWORKING_THREADS=$NUM_NETWORKING_THREADS \
	UID=$UID GID=$GID \
	COLLECT_WORKER_STATS=$COLLECT_WORKER_STATS \
	docker-compose -p $PROJECT_NAME -f DistributedSaturationBenchmark.yaml up \
	--abort-on-container-exit --exit-code-from control-node
  done
done








