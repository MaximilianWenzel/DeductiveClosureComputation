# Deductive Closure Computation
This repository has been used in the context of a university project, in which we examined whether we can efficiently compute the deductive closure for a given set of axioms and rules in a distributed fashion. In our approach, a control node initializes a set of worker nodes, which afterwards exchange newly derived conclusions autonomously with one another over the network. In order to determine whether all workers converged, we use a special convergence protocol, which is based on the total number of sent and received axioms in the procedure. 

## Approaches in the Experiments 
The different approaches, which were compared in the appropriate experiments, are the following:

- **Single-threaded**: Represents an implementation of the semi-naive deductive closure computation algorithm which is executed with a single thread. 
- **Multi-threaded**: Represents a parallelized implementation of the semi-naive deductive closure computation algorithm, which is executed in a single process. We use a control node and multiple worker nodes, which get executed in separate threads. The conclusions are distributed by adding them directly to the respective worker to-do queue. In order to determine whether all worker nodes found a fixpoint, we use our convergence protocol that counts the total number of sent and received axioms in the complete procedure.  
- **Distributed, multi-threaded**: Implementation of the distributed deductive closure computation procedure, where the control node and each worker node is executed in a separate thread. All messages are sent over the local network.
- **Distributed, in separate JVMs**: Distributed approach, where the control node and each worker node is executed in a separate JVM.  
- **Distributed, in separate docker containers**: Distributed approach, where the control node and each worker node is executed in a separate docker container. The communication between the control node and the worker nodes takes place in a virtual docker network.

## Execution of the Experiments 

In order to run the experiments, use the Apache Maven `mvn install` command in the directory of the *pom.xml* file to generate the appropriate JAR that contains all dependencies. Afterwards, place the JAR file and the *dockerBuild.sh* script in the same directory in order to generate the respective control node and worker docker images. 

### Docker Benchmark
Executes a benchmark, where the control node and each worker node get executed in a separate docker container. The workers communicate over a virtual docker network. Place the *dockerBenchmark.sh* script and docker compose file *DistributedSaturationBenchmark.yaml* in the same directory. Subsequently, run the *dockerBenchmark.sh* script. The time measurements are written to the home directory of the current user. 

### Single Machine Benchmarks
Executes a benchmark, where the control node and all worker nodes get executed in a single docker container. Included are all approaches except for the variant where each worker is executed in a separate docker container. After the docker images have been generated, simply run the *dockerBenchmark.sh* script. The time measurements are, again, written to the home directory of the current user. 