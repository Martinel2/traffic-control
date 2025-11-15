import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        // 1. 30초 동안 사용자를 0명에서 200명까지 서서히 늘립니다 (Ramp-up)
        { duration: '30s', target: 200 },
        // 2. 1분 동안 200명 상태를 유지합니다 (Hold)
        { duration: '1m', target: 200 },
        // 3. 10초 동안 사용자를 0명으로 줄입니다 (Ramp-down)
        { duration: '10s', target: 0 },
    ],
};

export default function () {
    // 우리가 만든 '최악의' API를 호출합니다 (optimized=false)
    const res = http.get('http://localhost:8080/api/process');

    // 응답이 성공(200 OK)했는지 확인합니다.
    check(res, { 'status was 200': (r) => r.status == 200 });
    sleep(1); // 1초 대기 후 다음 요청
}