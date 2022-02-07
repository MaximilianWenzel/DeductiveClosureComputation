#!/usr/bin/env bash
DOCKER_BUILDKIT=0 docker build -f "SaturationWorker.dockerfile" -t saturation-worker .
DOCKER_BUILDKIT=0 docker build -f "SaturationControlNode.dockerfile" -t saturation-control-node .