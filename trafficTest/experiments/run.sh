#!/usr/bin/env bash
# 한 번의 측정: 컨테이너를 새로 띄우고, 적용된 JVM 옵션을 기록한 뒤 k6 부하를 주고 GC 지표를 모은다.
# 사용법: experiments/run.sh <결과이름> "<JVM_OPTS>"   (trafficTest 디렉터리에서 실행)
set -euo pipefail
NAME="$1"; OPTS="${2:-}"
OUT="experiments/results/$NAME"; mkdir -p "$OUT"
PROJECT=tcexp
metric() { curl -s localhost:8080/actuator/prometheus; }
gc_sum() { metric | awk -v m="$1" '$1 ~ "^"m"\\{" {s+=$NF} END {printf "%.6f", s+0}'; }

JVM_OPTS="$OPTS" docker compose -p $PROJECT up -d --build --force-recreate my-api-server > /dev/null 2>&1
for _ in $(seq 1 90); do curl -s localhost:8080/actuator/health | grep -q UP && break; sleep 2; done
docker exec my-api-server jcmd 1 VM.flags > "$OUT/vm_flags.txt"
curl -s localhost:8080/api/process > "$OUT/api_check.json"

c0=$(gc_sum jvm_gc_pause_seconds_count); s0=$(gc_sum jvm_gc_pause_seconds_sum)
# 힙 사용량을 2초마다 기록 (Prometheus 15초 수집보다 촘촘하게)
( while true; do metric | awk '$1 ~ /^jvm_memory_used_bytes\{.*area="heap"/ {s+=$NF} END {print s+0}'; sleep 2; done ) > "$OUT/heap_used.txt" &
SAMPLER=$!
docker run --rm --network ${PROJECT}_monitoring-net -e BASE_URL=http://my-api-server:8080 \
  -v "$PWD":/w -w /w grafana/k6:0.54.0 run -q --summary-export "$OUT/k6_summary.json" k6_test.js > "$OUT/k6_stdout.txt" 2>&1
kill $SAMPLER
c1=$(gc_sum jvm_gc_pause_seconds_count); s1=$(gc_sum jvm_gc_pause_seconds_sum)
awk -v c0="$c0" -v c1="$c1" -v s0="$s0" -v s1="$s1" 'BEGIN {printf "{\"gc_pause_count\": %d, \"gc_pause_total_s\": %.6f}\n", c1-c0, s1-s0}' > "$OUT/gc_delta.json"
docker compose -p $PROJECT down > /dev/null 2>&1
echo "done $NAME"
