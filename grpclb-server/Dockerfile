FROM openjdk:8

COPY . /app/

WORKDIR /app

RUN ./gradlew :installDist

ENV GOOGLE_CLOUD_PROJECT grpc-kubecon-demo2017

ENTRYPOINT /app/build/install/app/bin/app
