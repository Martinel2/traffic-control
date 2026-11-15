// experiments/results/<조건>-r<회차>/ 결과를 조건별로 모아 Markdown 표로 출력한다.
// 사용법: node experiments/summarize.mjs   (trafficTest 디렉터리에서 실행)
import { readdirSync, readFileSync } from 'node:fs';

const dir = 'experiments/results';
const groups = {};
for (const name of readdirSync(dir).sort()) {
  const base = `${dir}/${name}`;
  const k6 = JSON.parse(readFileSync(`${base}/k6_summary.json`, 'utf8')).metrics;
  const gc = JSON.parse(readFileSync(`${base}/gc_delta.json`, 'utf8'));
  const heap = readFileSync(`${base}/heap_used.txt`, 'utf8').trim().split('\n').map(Number);
  const flags = readFileSync(`${base}/vm_flags.txt`, 'utf8');
  const cond = name.replace(/-r\d+$/, '');
  (groups[cond] ??= []).push({
    p95: k6.http_req_duration['p(95)'],
    max: k6.http_req_duration.max,
    reqs: k6.http_reqs.count,
    failed: k6.http_req_failed.value,
    gcCount: gc.gc_pause_count,
    gcTotalMs: gc.gc_pause_total_s * 1000,
    heapPeakMB: Math.max(...heap) / 1024 / 1024,
    maxHeapMB: Number(flags.match(/MaxHeapSize=(\d+)/)[1]) / 1024 / 1024,
    gcName: flags.includes('+UseShenandoahGC') ? 'Shenandoah' : flags.includes('+UseG1GC') ? 'G1' : '?',
  });
}

// 회차별 값을 "평균 (최소~최대)"로 보여 준다. 편차를 함께 봐야 차이가 의미 있는지 판단할 수 있다.
const fmt = (xs, d = 1) => {
  const avg = xs.reduce((a, b) => a + b, 0) / xs.length;
  return `${avg.toFixed(d)} (${Math.min(...xs).toFixed(d)}~${Math.max(...xs).toFixed(d)})`;
};
console.log('| 조건 | 회차 | 적용된 최대 힙 / GC | p95 ms | 최대 지연 ms | 요청 수 | 실패율 | GC 멈춤 횟수 | GC 멈춤 합계 ms | 힙 사용 최대 MB |');
console.log('|---|---|---|---|---|---|---|---|---|---|');
for (const [cond, rs] of Object.entries(groups)) {
  const col = (k, d) => fmt(rs.map((r) => r[k]), d);
  console.log(`| ${cond} | ${rs.length} | ${rs[0].maxHeapMB}MB / ${rs[0].gcName} | ${col('p95')} | ${col('max')} | ${col('reqs', 0)} | ${col('failed', 3)} | ${col('gcCount', 0)} | ${col('gcTotalMs')} | ${col('heapPeakMB')} |`);
}
