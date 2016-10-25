#!/bin/bash

sudo docker kill persistor server test-client integration-db
sudo docker rm persistor server test-client integration-db