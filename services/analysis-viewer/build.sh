#!/bin/bash

set -e

NAME=analysis-viewer-service

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
  yarn deps
  yarn shadow-cljs release analysis-viewer
  docker build --tag $IMG -f services/analysis-viewer/Dockerfile .
fi

# PUSH

if [ $PUSH = true ]
then

  REGISTRY=a8p1v4e1
  echo "Pushing $IMG to the registry $REGISTRY"

  # authenticate docker to use AWS registry
  aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws/$REGISTRY
  # tag image
  docker tag $IMG public.ecr.aws/$REGISTRY/$NAME:$TAG
  # docker tag $IMG public.ecr.aws/$REGISTRY/$NAME:$CIRCLE_SHA1
  # push tagged image to the registry
  docker push public.ecr.aws/$REGISTRY/$NAME:$TAG

fi

echo "Done"
exit $?
