#!/usr/bin/env sh
# Display a pastel rainbow on the whole rig
# (or what used to be the whole rig, if you've
# added lights).
curl -X POST -d @pastel_rainbow.json http://localhost:8080/mixer0/layer2/effect
