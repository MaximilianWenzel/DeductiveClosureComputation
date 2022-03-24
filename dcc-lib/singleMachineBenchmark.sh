#!/bin/bash
export UID=$(id -u)
export GID=$(id -g)

NUM_NETWORKING_THREADS=1
PROJECT_NAME=dcc-lib
COLLECT_WORKER_STATS=false

# JVM benchmark
for NUM_WORKERS in 1 2 3 4 5 6 7 8 # 9 10 11 12
do
  for BENCHMARK in binarytree echo  
  do
	docker run \
	--user $UID:$GID \
	-v ~/:/saturation \
	saturation-control-node single-machine $NUM_WORKERS $BENCHMARK $COLLECT_WORKER_STATS
  done
done