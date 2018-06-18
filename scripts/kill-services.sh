#!/bin/bash

pkill -f "com.aitusoftware.flute" || echo "Nothing killed"
DB_DIR="/tmp/$USER/flute-db"

rm -rf "$DB_DIR"