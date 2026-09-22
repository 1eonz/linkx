#!/bin/bash
cd /workspace || (echo "no dir /workspace found" ;exit 1)
mvn -s /workspace/settings.xml -DskipTests=true clean package
