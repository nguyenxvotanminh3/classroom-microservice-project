import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 20,           // 20 người dùng ảo
    duration: '1m',    // chạy trong 1 phút
};

export default function () {
    let payload = JSON.stringify({
        name: 'Test Classroom',
        description: 'Created by K6 performance test',
        teacherId: 1 // đảm bảo teacherId này hợp lệ
    });

    let params = {
        headers: {
            'Content-Type': 'application/json',
            // 'Authorization': 'Bearer <token>' // nếu có xác thực
        },
    };

    let res = http.post('http://localhost:8080/classrooms', payload, params);

    check(res, {
        'status is 201 or 200': (r) => r.status === 201 || r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1); // nghỉ 1s giữa các request
}
