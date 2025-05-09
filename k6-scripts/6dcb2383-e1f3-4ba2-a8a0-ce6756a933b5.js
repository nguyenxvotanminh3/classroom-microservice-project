import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 20, // Số lượng người dùng ảo
    duration: '1m', // Thời gian kiểm thử
};

export default function () {
    let payload = JSON.stringify({
        userName: 'testuser',
        password: 'testpass'
    });

    let params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    let res = http.post('http://localhost:8084/api/workflow/start', payload, params);
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });
    sleep(1);
}