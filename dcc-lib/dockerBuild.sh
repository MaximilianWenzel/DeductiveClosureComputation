#!/usr/bin/env bash
docker build -f "SaturationWorker.dockerfile" -t saturation-worker .
docker build -f "SaturationControlNode.dockerfile" -t saturation-control-node .