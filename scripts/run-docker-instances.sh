#!/bin/bash

sudo docker run -d -p 9092:9092 --name integration-db flute-integration-db

nc -w 10 localhost 9092 < /dev/null >/dev/null

if [ "$?" != "0" ]
then
    echo "Database server did not become available on port 9092"
    exit 1
fi


sudo docker run -d -p 51001:51001/udp -p 51000:51000/tcp --name persistor --link integration-db:integration-db flute-persistor

sudo docker run -d -p 15002:15002 -p 5006:5006 --name server --link integration-db:integration-db flute-server

nc -w 10 localhost 15002 < /dev/null >/dev/null

if [ "$?" != "0" ]
then
    echo "HTTP server did not become available on port 15002"
    exit 1
fi


sudo docker run --rm --name test-client --link persistor:persistor flute-test-client