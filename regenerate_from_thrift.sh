#!/bin/bash

echo generating new code
thrift --gen java chat.thrift

echo removing old server thrift
rm -rf server/src/thrift

echo copying new thrift to server
cp -r gen-java/thrift server/src/thrift

echo removing old android thrift
rm -rf android/app/src/main/java/thrift

echo copying new thrift to android
cp -r gen-java/thrift android/app/src/main/java/thrift

echo removing gen-java
rm -rf gen-java