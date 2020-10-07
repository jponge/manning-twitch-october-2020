#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "💥 Simulating traffic delays"

pumba netem \
  --duration 60s \
  --tc-image gaiadocker/iproute2 \
  delay --distribution pareto --time 3000 --jitter 500 \
  manning-twitch-october-2020_postgres_1

echo "🏁 Done with traffic delays"
