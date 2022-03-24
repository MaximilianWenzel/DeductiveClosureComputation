FROM openjdk:11
COPY ./dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar
ENTRYPOINT ["java", "-Xmx4G", "-cp", "dcc-lib-1.0-LOCALBUILD-jar-with-dependencies.jar", "reasoning.saturation.distributed.SaturationWorker"]