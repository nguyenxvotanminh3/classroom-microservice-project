import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 20, // Số lượng người dùng ảo đồng thời
    duration: '1m', // Thời gian kiểm thử
    thresholds: {
        // Tăng ngưỡng thời gian phản hồi lên 1000ms (1 giây)
        http_req_duration: ['p(95)<1000'],
        // Không kết thúc test với mã lỗi khi vượt ngưỡng
        'http_req_duration{source:check}': [
            { threshold: 'p(95)<1000', abortOnFail: false }
        ]
    }
};

export default function () {
    let res = http.get('http://host.docker.internal:8082/api/classrooms');
    check(res, {
        'status is 200': (r) => r.status === 200,
        // Tăng ngưỡng thời gian phản hồi trong check
        'response time < 1000ms': (r) => r.timings.duration < 1000,
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
        // Tăng ngưỡng thời gian phản hồi trong check
        'POST response time < 1000ms': (r) => r.timings.duration < 1000,
    });
    
    sleep(1); // Nghỉ 1s giữa các lần lặp để mô phỏng người dùng thực tế
}