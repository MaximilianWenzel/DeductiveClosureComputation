FROM openjdk:11
COPY ./target/dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar
ENTRYPOINT ["java", "-cp", "dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar", "benchmark.DockerSaturationBenchmark"]