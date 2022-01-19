for APPROACH in single-machine docker-network
do
  for NUM_WORKES in 1 2 3 4 5 6 7 8
  do
    for BENCHMARK in echo binarytree
    do
      APPROACH=$APPROACH NUM_WORKERS=$NUM_WORKERS BENCHMARK=echo \
      docker-compose -f DistributedSaturationBenchmark.yaml up \
      --abort-on-container-exit --exit-code-from control-node
    done
  done
done