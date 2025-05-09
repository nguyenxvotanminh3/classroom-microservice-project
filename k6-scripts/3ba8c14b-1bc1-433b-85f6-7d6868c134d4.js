import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const DURATION = __ENV.DURATION || 30; // in seconds
const VUS = __ENV.VUS || 10; // virtual users

export const options = {
  stages: [
    { duration: '10s', target: VUS },    // Ramp up to target VUs
    { duration: '20s', target: VUS*2 },  // Double the load to trigger circuit breaker
    { duration: '20s', target: VUS*2 },  // Stay at high load
    { duration: '10s', target: VUS },    // Scale back to normal
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'], // Allow up to 50% failure (circuit breaker may be open)
  },
};

export default function() {
  // 1. Make a few calls to the service
  let response = http.get(BASE_URL + '/api/circuit-breaker-test/success-count');
  check(response, {
    'success count request successful': (r) => r.status === 200,
  });
  
  // 2. Try to trigger the circuit breaker with slow requests
  response = http.post(BASE_URL + '/api/circuit-breaker-test/slow-requests?delaySeconds=2');
  
  // 3. Check the circuit breaker state
  response = http.get(BASE_URL + '/api/circuit-breaker-monitor/names');
  
  if (response.status === 200) {
    const data = response.json();
    if (data.circuitBreakers && data.circuitBreakers.length > 0) {
      // Get the first circuit breaker
      const cbName = data.circuitBreakers[0];
      
      // Get details for this circuit breaker
      response = http.get(BASE_URL + '/api/circuit-breaker-monitor/' + cbName);
      
      check(response, {
        'circuit breaker details retrieved': (r) => r.status === 200,
        'circuit breaker state is valid': (r) => {
          const state = r.json().state;
          return state === 'CLOSED' || state === 'OPEN' || state === 'HALF_OPEN';
        },
      });
    }
  }
  
  sleep(1);
}