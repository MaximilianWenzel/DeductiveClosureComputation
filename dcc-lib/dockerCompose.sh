for APPROACH in single-machine docker-network
do
  for NUM_WORKERS in  2
  do
    for BENCHMARK in echo
    do
      APPROACH=$APPROACH NUM_WORKERS=$NUM_WORKERS BENCHMARK=echo \
      docker-compose -f DistributedSaturationBenchmark.yaml up \
      --abort-on-container-exit --exit-code-from control-node
    done
  done
done