import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 20, // Số lượng người dùng ảo đồng thời
    duration: '1m', // Thời gian kiểm thử
};

export default function () {
    let res = http.get('http://host.docker.internal:8082/api/classrooms');
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });
    
    const payload = JSON.stringify({
        name: 'Test Classroom',
        capacity: 30
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };
    
    let postRes = http.post('http://host.docker.internal:8082/api/classrooms', payload, params);
    check(postRes, {
        'POST status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'POST response time < 500ms': (r) => r.timings.duration < 500,
    });
    
    sleep(1); // Nghỉ 1s giữa các lần lặp để mô phỏng người dùng thực tế
}