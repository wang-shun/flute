#!/bin/bash

sudo docker build -t flute-persistor -f ./persistor/config/docker/persistor-Dockerfile . && \
sudo docker build -t flute-integration-db -f ./persistor/config/docker/testdb-Dockerfile . && \
sudo docker build -t flute-test-client -f ./integration/config/docker/clientApplication-Dockerfile . && \
sudo docker build -t flute-server -f ./server/config/docker/server-Dockerfile .