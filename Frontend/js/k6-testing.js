/**
 * K6 Performance Testing Module
 */

// Test templates
const templates = {
    'custom-test': `import http from 'k6/http';
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
}`,

    'load-test': `import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API_PATH = '/api/circuit-breaker-monitor/all';
const DURATION = __ENV.DURATION || 30; // in seconds
const VUS = __ENV.VUS || 10; // virtual users
const RAMPUP = __ENV.RAMPUP || 5; // ramp-up time in seconds

export const options = {
  stages: [
    { duration: RAMPUP + 's', target: VUS }, // Ramp up to target VUs
    { duration: (DURATION - RAMPUP) + 's', target: VUS }, // Stay at target VUs
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.1'], // Less than 10% of requests should fail
  },
};

export default function() {
  const url = BASE_URL + API_PATH;
  
  const response = http.get(url);
  
  check(response, {
    'is status 200': (r) => r.status === 200,
    'has circuit breakers': (r) => r.json().circuitBreakers && r.json().circuitBreakers.length > 0,
  });
  
  sleep(1);
}`,

    'stress-test': `import http from 'k6/http';
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
}`,

    'spike-test': `import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API_PATH = '/api/circuit-breaker-monitor/all';
const DURATION = __ENV.DURATION || 60; // in seconds
const VUS = __ENV.VUS || 100; // virtual users during spike

export const options = {
  stages: [
    { duration: '5s', target: 10 },     // Normal load
    { duration: '10s', target: VUS },   // Spike to high load
    { duration: '10s', target: VUS },   // Stay at high load
    { duration: '10s', target: 10 },    // Scale back to normal
    { duration: '25s', target: 10 },    // Stay at normal load
    { duration: '5s', target: 0 },      // Scale down to zero
  ],
  thresholds: {
    http_req_failed: ['rate<0.3'], // Less than 30% of requests should fail
  },
};

export default function() {
  const url = BASE_URL + API_PATH;
  
  const response = http.get(url);
  
  check(response, {
    'is status 200': (r) => r.status === 200,
  });
  
  // Short sleep to make the test more realistic
  sleep(0.1);
}`,

    'soak-test': `import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API_PATH = '/api/circuit-breaker-monitor/all';
const DURATION = __ENV.DURATION || 300; // in seconds (5 minutes default)
const VUS = __ENV.VUS || 20; // virtual users

export const options = {
  stages: [
    { duration: '1m', target: VUS },     // Ramp up
    { duration: (DURATION - 120) + 's', target: VUS }, // Stay at load
    { duration: '1m', target: 0 },      // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1500'],
    http_req_failed: ['rate<0.05'], // Less than 5% of requests should fail
  },
};

export default function() {
  const url = BASE_URL + API_PATH;
  
  const response = http.get(url);
  
  check(response, {
    'is status 200': (r) => r.status === 200,
  });
  
  sleep(1);
}`,

    'circuit-breaker': `import http from 'k6/http';
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
}`
};

// Charts and data
let responseTimeChart;
let rpsChart;
let testResults = {
    successRate: 0,
    avgResponseTime: 0,
    totalRequests: 0,
    rps: 0,
    responseTimeSeries: [],
    rpsSeries: [],
    timestamps: []
};

// DOM elements
let editor;
let runButton;
let loadingOverlay;
let loadingStatus;

// Initialize the module
document.addEventListener('DOMContentLoaded', function() {
    // Initialize CodeMirror
    const scriptArea = document.getElementById('k6Script');
    if (scriptArea) {
        editor = CodeMirror.fromTextArea(scriptArea, {
            mode: 'javascript',
            theme: 'dracula',
            lineNumbers: true,
            lineWrapping: true,
            tabSize: 2,
            indentWithTabs: false,
            autoCloseBrackets: true,
            matchBrackets: true
        });
        
        // Load default template - change từ 'circuit-breaker' thành 'custom-test'
        editor.setValue(templates['custom-test']);
    }
    
    // Set up event listeners
    setupEventListeners();
    
    // Initialize charts
    initializeCharts();
});

// Set up event listeners
function setupEventListeners() {
    // Run K6 test button
    runButton = document.getElementById('runK6Test');
    if (runButton) {
        runButton.addEventListener('click', function() {
            runK6Test();
        });
    }
    
    // Save script button
    const saveButton = document.getElementById('saveK6Script');
    if (saveButton) {
        saveButton.addEventListener('click', function() {
            saveK6Script();
        });
    }
    
    // Template selector
    const templateLinks = document.querySelectorAll('.template-selector');
    templateLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const templateName = this.getAttribute('data-template');
            if (templates[templateName]) {
                if (confirm('This will replace your current script. Continue?')) {
                    editor.setValue(templates[templateName]);
                }
            }
        });
    });
    
    // Loading overlay
    loadingOverlay = document.getElementById('loadingOverlay');
    loadingStatus = document.getElementById('loadingStatus');
}

// Initialize charts
function initializeCharts() {
    const responseTimeCtx = document.getElementById('responseTimeChart').getContext('2d');
    responseTimeChart = new Chart(responseTimeCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Response Time (ms)',
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                borderColor: 'rgba(75, 192, 192, 1)',
                borderWidth: 2,
                pointRadius: 0,
                pointHitRadius: 10,
                data: []
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Time (s)'
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Response Time (ms)'
                    },
                    beginAtZero: true
                }
            }
        }
    });
    
    const rpsCtx = document.getElementById('rpsChart').getContext('2d');
    rpsChart = new Chart(rpsCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Requests per Second',
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 2,
                pointRadius: 0,
                pointHitRadius: 10,
                data: []
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Time (s)'
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Requests/Second'
                    },
                    beginAtZero: true
                }
            }
        }
    });
}

// Run K6 test
async function runK6Test() {
    try {
        // Đảm bảo reset kết quả trước
        resetResults();
        
        // Show loading overlay with animation
        showLoading('Preparing test script...');
        
        // Kích hoạt lại animation sau một khoảng thời gian ngắn
        setTimeout(() => {
            showLoading('Preparing test script...');
        }, 50);
        
        // Get configuration
        const testDuration = document.getElementById('testDuration').value;
        const vus = document.getElementById('vus').value;
        const rampupTime = document.getElementById('rampupTime').value;
        
        // Get the script
        const script = editor.getValue();
        
        // Disable run button
        runButton.disabled = true;
        
        // Hiển thị loading với thông báo mới
        showLoading('Submitting test to K6...');
        
        // Submit to backend
        const response = await fetch('/api/k6-test/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                script: script,
                env: {
                    DURATION: testDuration,
                    VUS: vus,
                    RAMPUP: rampupTime
                }
            })
        });
        
        if (!response.ok) {
            throw new Error(`Server responded with status ${response.status}`);
        }
        
        // Get test ID from response
        const data = await response.json();
        const testId = data.testId;
        
        // Start polling for test results
        await pollTestResults(testId);
        
    } catch (error) {
        console.error('Failed to run K6 test:', error);
        showError('Failed to run K6 test: ' + error.message);
    } finally {
        // Hide loading
        hideLoading();
        runButton.disabled = false;
    }
}

// Poll for test results
async function pollTestResults(testId) {
    return new Promise((resolve, reject) => {
        const poll = async () => {
            try {
                showLoading('Running K6 test... Please wait.');
                
                const response = await fetch(`/api/k6-test/status/${testId}`);
                if (!response.ok) {
                    throw new Error(`Server responded with status ${response.status}`);
                }
                
                const data = await response.json();
                
                // Update results even during running state
                updateResults(data);
                
                // If test is completed, resolve the promise
                if (data.status === 'completed') {
                    showLoading('Test completed! Processing results...');
                    setTimeout(() => {
                        hideLoading();
                        resolve();
                    }, 1000);
                } else if (data.status === 'running') {
                    // Update loading status with progress
                    const progress = data.progress || 0;
                    showLoading(`Running K6 test... ${progress.toFixed(0)}% complete`);
                    
                    // Continue polling but at a faster rate
                    setTimeout(poll, 300); // Poll every 300ms instead of 500ms for more responsive UI
                } else if (data.status === 'failed') {
                    throw new Error(data.error || 'Test failed');
                } else {
                    // Some other status, keep polling
                    setTimeout(poll, 1000);
                }
                
            } catch (error) {
                console.error('Error polling test results:', error);
                hideLoading();
                reject(error);
            }
        };
        
        // Start polling
        poll();
    });
}

// Update test results
function updateResults(data) {
    // If we have metrics data or partial data during test run, update the UI
    if (data.metrics || data.status === 'running') {
        // Show results container, hide placeholder
        document.getElementById('resultsPlaceholder').style.display = 'none';
        document.getElementById('resultsContent').style.display = 'block';
        
        // Update summary stats
        const metrics = data.metrics || {};
        
        // Calculate success rate and other metrics
        testResults.successRate = 100 - (metrics.http_req_failed?.rate || 0) * 100;
        testResults.avgResponseTime = metrics.http_req_duration?.avg || 0;
        testResults.totalRequests = metrics.http_reqs?.count || 0;
        testResults.rps = metrics.http_reqs?.rate || 0;
        
        // Update UI with current metrics
        document.getElementById('successRate').textContent = testResults.successRate.toFixed(2) + '%';
        document.getElementById('avgResponseTime').textContent = testResults.avgResponseTime.toFixed(2) + 'ms';
        document.getElementById('totalRequests').textContent = testResults.totalRequests;
        document.getElementById('rps').textContent = testResults.rps.toFixed(2);
        
        // Hiển thị cảnh báo nếu có threshold bị vượt quá
        if (data.thresholdWarning) {
            // Tạo hoặc cập nhật phần tử cảnh báo
            let warningEl = document.getElementById('thresholdWarning');
            if (!warningEl) {
                warningEl = document.createElement('div');
                warningEl.id = 'thresholdWarning';
                warningEl.className = 'alert alert-warning mt-3';
                warningEl.innerHTML = `<i class="fas fa-exclamation-triangle me-2"></i> ${data.thresholdWarning}`;
                
                // Thêm vào sau phần kết quả tóm tắt
                const resultsContent = document.getElementById('resultsContent');
                resultsContent.insertBefore(warningEl, resultsContent.firstChild.nextSibling);
            } else {
                warningEl.innerHTML = `<i class="fas fa-exclamation-triangle me-2"></i> ${data.thresholdWarning}`;
            }
        }
        
        // Update progress in UI if test is still running
        if (data.status === 'running' && data.progress) {
            // Add progress indicator to each metric
            const progressText = ` (${data.progress.toFixed(0)}% complete)`;
            
            // Optionally add a progress indicator to each card
            const metricLabels = document.querySelectorAll('.metric-label');
            if (metricLabels.length > 0) {
                const firstLabel = metricLabels[0];
                if (!firstLabel.textContent.includes('complete')) {
                    firstLabel.textContent += progressText;
                }
            }
        }
        
        // Update detailed metrics table
        updateMetricsTable(metrics);
        
        // If we have time-series data, update the charts
        if (data.timeSeries) {
            updateCharts(data.timeSeries);
        }
    }
}

// Update detailed metrics table
function updateMetricsTable(metrics) {
    const tableBody = document.getElementById('metricsTableBody');
    if (!tableBody) return;
    
    // Clear existing rows
    tableBody.innerHTML = '';
    
    // Add rows for each metric
    for (const [metricName, metricValues] of Object.entries(metrics)) {
        // Skip non-object metrics
        if (typeof metricValues !== 'object') continue;
        
        // Create row for metric
        const row = document.createElement('tr');
        
        const nameCell = document.createElement('td');
        nameCell.textContent = formatMetricName(metricName);
        row.appendChild(nameCell);
        
        const valueCell = document.createElement('td');
        valueCell.innerHTML = formatMetricValues(metricValues);
        row.appendChild(valueCell);
        
        tableBody.appendChild(row);
    }
}

// Format metric name
function formatMetricName(name) {
    return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

// Format metric values
function formatMetricValues(values) {
    let result = '';
    
    for (const [key, value] of Object.entries(values)) {
        if (key === 'rate') {
            result += `Rate: ${value.toFixed(2)}/s<br>`;
        } else if (key === 'avg') {
            result += `Avg: ${value.toFixed(2)}ms<br>`;
        } else if (key === 'med') {
            result += `Median: ${value.toFixed(2)}ms<br>`;
        } else if (key === 'p95') {
            result += `P95: ${value.toFixed(2)}ms<br>`;
        } else if (key === 'p99') {
            result += `P99: ${value.toFixed(2)}ms<br>`;
        } else if (key === 'count') {
            result += `Count: ${value}<br>`;
        } else if (key === 'min') {
            result += `Min: ${value.toFixed(2)}ms<br>`;
        } else if (key === 'max') {
            result += `Max: ${value.toFixed(2)}ms<br>`;
        } else {
            result += `${key}: ${value}<br>`;
        }
    }
    
    return result;
}

// Update charts with time-series data
function updateCharts(timeSeries) {
    if (!responseTimeChart || !rpsChart) return;
    
    // Extract data points
    const timestamps = timeSeries.timestamps || [];
    const responseTimeSeries = timeSeries.http_req_duration || [];
    const rpsSeries = timeSeries.http_reqs || [];
    
    // Update response time chart
    responseTimeChart.data.labels = timestamps.map(t => (t / 1000).toFixed(0)); // Convert to seconds
    responseTimeChart.data.datasets[0].data = responseTimeSeries;
    responseTimeChart.update();
    
    // Update RPS chart
    rpsChart.data.labels = timestamps.map(t => (t / 1000).toFixed(0)); // Convert to seconds
    rpsChart.data.datasets[0].data = rpsSeries;
    rpsChart.update();
}

// Reset results
function resetResults() {
    // Reset test results
    testResults = {
        successRate: 0,
        avgResponseTime: 0,
        totalRequests: 0,
        rps: 0,
        responseTimeSeries: [],
        rpsSeries: [],
        timestamps: []
    };
    
    // Hide results, show placeholder
    document.getElementById('resultsPlaceholder').style.display = 'block';
    document.getElementById('resultsContent').style.display = 'none';
    
    // Reset charts
    if (responseTimeChart) {
        responseTimeChart.data.labels = [];
        responseTimeChart.data.datasets[0].data = [];
        responseTimeChart.update();
    }
    
    if (rpsChart) {
        rpsChart.data.labels = [];
        rpsChart.data.datasets[0].data = [];
        rpsChart.update();
    }
}

// Save K6 script
function saveK6Script() {
    const script = editor.getValue();
    const blob = new Blob([script], { type: 'text/javascript' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = 'k6-test-script.js';
    a.click();
    
    URL.revokeObjectURL(url);
}

// Show loading overlay
function showLoading(message) {
    // Đảm bảo spinner visible
    loadingOverlay.style.display = 'flex';
    
    // Cập nhật message
    if (loadingStatus) {
        loadingStatus.textContent = message || 'Loading...';
    }
    
    // Đảm bảo animation hoạt động bằng cách set lại animationPlayState nếu cần
    const spinner = loadingOverlay.querySelector('.spinner-border');
    if (spinner) {
        // Force reflow to ensure animation restarts if paused
        spinner.style.animationPlayState = 'running';
        // Force reflow để trigger lại animation
        void spinner.offsetWidth;
    }
}

// Hide loading overlay
function hideLoading() {
    loadingOverlay.style.display = 'none';
}

// Show error message
function showError(message) {
    alert(message);
} 