#!/bin/bash

set -e

NAME=api-service

while getopts b:p:t: flag
do
  case "${flag}" in
    b) BUILD=${OPTARG};;
    p) PUSH=${OPTARG};;
    t) TAG=${OPTARG};;
  esac
done

# defaults

if [ -z ${TAG+x} ];
then
  TAG=latest
fi

if [ -z ${BUILD+x} ];
then
  BUILD=true
fi

if [ -z ${PUSH+x} ];
then
  PUSH=false
fi

# BUILD

IMG=$NAME:$TAG

if [ $BUILD = true ]
then
    cd ../../
    # TODO : this overwrites libspread pom.xml in the root dir
    # not sure how to fix that (build libspread with deps.edn?)
    clojure -Spom;
    clojure -A:uberjar api-service.jar -C -m api.main;

    docker build --tag $IMG -f services/api/Dockerfile .
fi

# PUSH

if [ $PUSH = true ]; then

  REGISTRY=a8p1v4e1
  echo "Pushing $IMG to the registry $REGISTRY"

  # authenticate docker to use AWS registry
  aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws/$REGISTRY
  # tag image
  docker tag $IMG public.ecr.aws/$REGISTRY/$NAME:$TAG
  # push tagged image to the registry
  docker push public.ecr.aws/$REGISTRY/$NAME:$TAG

fi

echo "Done"
exit $?
