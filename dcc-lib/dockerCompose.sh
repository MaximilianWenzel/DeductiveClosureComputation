for APPROACH in single-machine docker-network
do
  APPROACH=$APPROACH NUM_WORKERS=2 BENCHMARK=echo \
  docker-compose -f DistributedSaturationBenchmark.yaml up \
  --abort-on-container-exit --exit-code-from control-node
done