import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API_PATH = '/api/circuit-breaker-test/stress-test';
const DURATION = __ENV.DURATION || 30; // in seconds
const MAX_VUS = __ENV.VUS || 50; // maximum virtual users

export const options = {
  stages: [
    { duration: '5s', target: 10 },
    { duration: '10s', target: Math.floor(MAX_VUS / 2) },
    { duration: '10s', target: MAX_VUS },
    { duration: '30s', target: MAX_VUS },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% of requests should be below 1000ms
    http_req_failed: ['rate<0.2'], // Less than 20% of requests should fail
  },
};

export default function() {
  const url = BASE_URL + API_PATH;
  
  const payload = JSON.stringify({
    concurrentRequests: 5,
    includeDuplicates: true
  });
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const response = http.post(url, payload, params);
  
  check(response, {
    'is status 200': (r) => r.status === 200,
  });
  
  sleep(1);
}