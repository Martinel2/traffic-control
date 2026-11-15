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
    // BASE_URL로 대상 서버를 바꿀 수 있습니다 (도커 네트워크 안에서 k6를 돌릴 때 http://my-api-server:8080)
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
    const res = http.get(`${baseUrl}/api/process`);

    // 응답이 성공(200 OK)했는지 확인합니다.
    check(res, { 'status was 200': (r) => r.status == 200 });
    sleep(1); // 1초 대기 후 다음 요청
}