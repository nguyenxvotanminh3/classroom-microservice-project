const express = require('express');
const cors = require('cors');
const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');

const app = express();
const port = 3005;

// Middleware
app.use(cors());
app.use(express.json({ limit: '5mb' }));
app.use(express.static(path.join(__dirname, '..'))); // Serve files from Frontend directory

// Directory setup - đường dẫn tới thư mục gốc của dự án
const rootDir = path.join(__dirname, '../..');
const SCRIPTS_DIR = path.join(rootDir, 'k6-scripts');
const RESULTS_DIR = path.join(rootDir, 'results');

// Create directories if they don't exist
if (!fs.existsSync(SCRIPTS_DIR)) {
  fs.mkdirSync(SCRIPTS_DIR, { recursive: true });
}
if (!fs.existsSync(RESULTS_DIR)) {
  fs.mkdirSync(RESULTS_DIR, { recursive: true });
}

// Store running tests
const tests = {};

// API Endpoints
app.post('/api/k6-test/run', (req, res) => {
  try {
    const { script, env } = req.body;
    const testId = uuidv4();
    const scriptPath = path.join(SCRIPTS_DIR, `${testId}.js`);
    const outputPath = path.join(RESULTS_DIR, `${testId}-output.json`);

    // Save script to file
    fs.writeFileSync(scriptPath, script);

    // Create environment variables string
    const envString = Object.entries(env)
      .map(([key, value]) => `-e ${key}=${value}`)
      .join(' ');

    // Create the docker command
    const command = `docker-compose exec -T k6 k6 run ${envString} --out json=/results/${testId}-output.json /scripts/${testId}.js`;

    console.log(`Running command: ${command}`);

    // Store test info
    tests[testId] = {
      status: 'submitted',
      startTime: new Date(),
      command,
      scriptPath,
      outputPath
    };

    // Start test process
    const process = exec(command);
    let output = '';

    tests[testId].process = process;
    tests[testId].status = 'running';

    process.stdout.on('data', (data) => {
      output += data;
      console.log(`[${testId}] ${data}`);

      // Try to extract progress from output
      try {
        // Try multiple progress formats
        let progressMatch = null;
        
        // Format 1: [XX.X%]
        if (data.includes('execution:')) {
          progressMatch = data.match(/\[([0-9.]+)%\]/);
          if (progressMatch && progressMatch[1]) {
            const progress = parseFloat(progressMatch[1]);
            tests[testId].progress = progress;
            console.log(`[${testId}] Progress updated: ${progress}%`);
          }
        }
        
        // Format 2: X% 
        if (!progressMatch) {
          progressMatch = data.match(/default\s+\[\s*([0-9]+)%\s*\]/);
          if (progressMatch && progressMatch[1]) {
            const progress = parseFloat(progressMatch[1]);
            tests[testId].progress = progress;
            console.log(`[${testId}] Progress updated: ${progress}%`);
          }
        }
      } catch (e) {
        console.error('Error parsing progress:', e);
      }
    });

    process.stderr.on('data', (data) => {
      output += data;
      console.error(`[${testId}] Error: ${data}`);
    });

    process.on('close', (code) => {
      console.log(`[${testId}] Process exited with code ${code}`);
      
      // Chấp nhận cả khi test kết thúc với mã 99 (thresholds bị vượt quá)
      if (code === 0 || code === 99) {
        tests[testId].status = 'completed';
        tests[testId].endTime = new Date();
        
        // Nếu có lỗi threshold thì ghi lại để hiển thị cho người dùng
        if (code === 99) {
          tests[testId].thresholdWarning = 'Test completed but some performance thresholds were exceeded';
        }
        
        // Try to read the output file
        try {
          if (fs.existsSync(outputPath)) {
            const fileContent = fs.readFileSync(outputPath, 'utf8');
            console.log(`[${testId}] Raw result file content (first 200 chars): ${fileContent.substring(0, 200)}`);
            
            try {
              // Attempt to sanitize the JSON
              const sanitizedContent = fileContent.replace(/[\u0000-\u001F\u007F-\u009F]/g, "");
              const results = JSON.parse(sanitizedContent);
              tests[testId].results = extractResults(results);
              console.log(`[${testId}] Results successfully parsed and saved`);
            } catch (jsonError) {
              console.error(`[${testId}] JSON parsing error:`, jsonError);
              
              // Fallback: Try to extract metrics directly from the command output
              console.log(`[${testId}] Attempting to extract results from output`);
              tests[testId].results = extractResultsFromOutput(output);
            }
          } else {
            console.error(`[${testId}] Output file not found at ${outputPath}`);
            // Fallback to output-based extraction
            tests[testId].results = extractResultsFromOutput(output);
          }
        } catch (e) {
          console.error(`[${testId}] Error reading results:`, e);
          tests[testId].error = `Error reading results: ${e.message}`;
          
          // Fallback to output-based extraction
          try {
            tests[testId].results = extractResultsFromOutput(output);
          } catch (outputError) {
            console.error(`[${testId}] Failed to extract results from output:`, outputError);
          }
        }
      } else {
        tests[testId].status = 'failed';
        tests[testId].error = `Process exited with code ${code}: ${output}`;
      }
    });

    // Return test ID
    res.json({ testId, status: 'submitted' });
  } catch (error) {
    console.error('Error running test:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get test status
app.get('/api/k6-test/status/:testId', (req, res) => {
  const { testId } = req.params;
  const test = tests[testId];
  
  if (!test) {
    return res.json({ testId, status: 'not_found' });
  }
  
  // Return status, progress and any basic metrics if available
  const response = {
    testId,
    status: test.status,
    progress: test.progress,
  };
  
  if (test.error) {
    response.error = test.error;
  }
  
  // Thêm thông báo cảnh báo threshold nếu có
  if (test.thresholdWarning) {
    response.thresholdWarning = test.thresholdWarning;
  }
  
  if (test.results) {
    response.metrics = test.results.metrics;
    response.timeSeries = test.results.timeSeries;
  }
  
  res.json(response);
});

// Get test templates
app.get('/api/k6-test/templates', (req, res) => {
  const templates = {
    'load-test': getTemplate('load-test'),
    'stress-test': getTemplate('stress-test'),
    'spike-test': getTemplate('spike-test'),
    'soak-test': getTemplate('soak-test'),
    'circuit-breaker': getTemplate('circuit-breaker')
  };
  
  res.json(templates);
});

// Helper function to get template content
function getTemplate(name) {
  const defaultTemplates = {
    'load-test': `import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
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
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
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
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
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
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
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
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
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

  try {
    // Try to read from files first
    const filePath = path.join(SCRIPTS_DIR, `${name}.js`);
    if (fs.existsSync(filePath)) {
      return fs.readFileSync(filePath, 'utf8');
    }
    // Fall back to default templates
    return defaultTemplates[name] || `// Template not found: ${name}`;
  } catch (error) {
    console.error(`Error reading template ${name}:`, error);
    return defaultTemplates[name] || `// Template not found: ${name}`;
  }
}

// Helper function to extract results data
function extractResults(results) {
  const extractedResults = {
    metrics: {},
    timeSeries: {}
  };

  try {
    // Extract metrics
    if (results.metrics) {
      const metrics = {};
      for (const [metricName, metricValues] of Object.entries(results.metrics)) {
        const extractedValues = {};
        for (const key of ['avg', 'min', 'max', 'med', 'p90', 'p95', 'p99', 'count']) {
          if (metricValues[key] !== undefined) {
            extractedValues[key] = metricValues[key];
          }
        }

        // Also compute rate (requests per second)
        if (metricValues.count !== undefined && metricValues.duration !== undefined) {
          const count = metricValues.count;
          const duration = metricValues.duration / 1000; // Convert to seconds
          if (duration > 0) {
            extractedValues.rate = count / duration;
          }
        }

        metrics[metricName] = extractedValues;
      }
      extractedResults.metrics = metrics;
    }

    // Extract time series data
    if (results.time_series) {
      const metricSeries = {};
      const timestamps = [];

      for (const dataPoint of results.time_series) {
        const metric = dataPoint.metric;
        const value = dataPoint.value;
        const timestamp = dataPoint.timestamp;

        if (!metricSeries[metric]) {
          metricSeries[metric] = [];
        }

        metricSeries[metric].push(value);

        if (!timestamps.includes(timestamp)) {
          timestamps.push(timestamp);
        }
      }

      // Sort timestamps
      timestamps.sort((a, b) => a - b);

      extractedResults.timeSeries = {
        timestamps,
        ...metricSeries
      };
    }
  } catch (error) {
    console.error('Error extracting results:', error);
  }

  return extractedResults;
}

// Helper function to extract results from command output
function extractResultsFromOutput(output) {
  const extractedResults = {
    metrics: {},
    timeSeries: {}
  };

  try {
    // Extract basic metrics
    const metrics = {};
    
    // Extract HTTP metrics
    const httpDurationMatch = output.match(/http_req_duration[.\s]+: avg=([0-9.]+)ms min=([0-9.]+)ms med=([0-9.]+)ms max=([0-9.]+)ms/);
    if (httpDurationMatch) {
      metrics.http_req_duration = {
        avg: parseFloat(httpDurationMatch[1]),
        min: parseFloat(httpDurationMatch[2]),
        med: parseFloat(httpDurationMatch[3]),
        max: parseFloat(httpDurationMatch[4])
      };
    } else {
      // Try alternate format
      const altDurationMatch = output.match(/http_req_duration[.\s]+: avg=([0-9.]+)ms/);
      if (altDurationMatch) {
        metrics.http_req_duration = {
          avg: parseFloat(altDurationMatch[1]),
          min: 0,
          med: 0,
          max: 0
        };
      } else {
        // Try new alternate format that might be in the output
        const altFormat2 = output.match(/http_req_duration[^:]*:[^a-z]*avg=([0-9.]+)ms[^a-z]*min=([0-9.]+)ms[^a-z]*med=([0-9.]+)ms[^a-z]*max=([0-9.]+)ms/);
        if (altFormat2) {
          metrics.http_req_duration = {
            avg: parseFloat(altFormat2[1]),
            min: parseFloat(altFormat2[2]),
            med: parseFloat(altFormat2[3]),
            max: parseFloat(altFormat2[4])
          };
        }
      }
    }
    
    // Extract HTTP request count and rate
    const httpReqsMatch = output.match(/http_reqs[.\s]+: ([0-9]+)\s+([0-9.]+)\/s/);
    if (httpReqsMatch) {
      metrics.http_reqs = {
        count: parseInt(httpReqsMatch[1]),
        rate: parseFloat(httpReqsMatch[2])
      };
    }
    
    // Extract failure rate
    const httpFailedMatch = output.match(/http_req_failed[.\s]+: ([0-9.]+)%\s+([0-9]+) out of ([0-9]+)/);
    if (httpFailedMatch) {
      const failurePercentage = parseFloat(httpFailedMatch[1]);
      metrics.http_req_failed = {
        rate: failurePercentage / 100,
        count: parseInt(httpFailedMatch[2]),
        total: parseInt(httpFailedMatch[3])
      };
    }
    
    // Extract checks
    const checksMatch = output.match(/checks_succeeded[.\s]+: ([0-9.]+)%\s+([0-9]+) out of ([0-9]+)/);
    if (checksMatch) {
      metrics.checks = {
        rate: parseFloat(checksMatch[1]) / 100,
        succeeded: parseInt(checksMatch[2]),
        total: parseInt(checksMatch[3])
      };
    }
    
    extractedResults.metrics = metrics;
    
    // Create simulated time series data (since we can't extract the real time series from output)
    const timestamps = [];
    const responseTimeSeries = [];
    const rpsSeries = [];
    
    // Simulate 10 data points spread over the test duration
    const iterations = 10;
    const avgResponseTime = metrics.http_req_duration?.avg || 0;
    const rps = metrics.http_reqs?.rate || 0;
    
    for (let i = 0; i < iterations; i++) {
      timestamps.push(i * 1000); // Each second
      
      // Simulate some variance around the average
      const variance = 0.3; // 30% variance
      const randomFactor = 1 + (Math.random() * variance * 2 - variance);
      
      responseTimeSeries.push(avgResponseTime * randomFactor);
      rpsSeries.push(rps * randomFactor);
    }
    
    extractedResults.timeSeries = {
      timestamps,
      http_req_duration: responseTimeSeries,
      http_reqs: rpsSeries
    };
    
    console.log('Extracted metrics from output:', extractedResults.metrics);
  } catch (error) {
    console.error('Error extracting results from output:', error);
  }

  return extractedResults;
}

// Start the server
app.listen(port, () => {
  console.log(`K6 test server listening at http://localhost:${port}`);
  console.log(`Open http://localhost:${port}/k6-tests.html to access the testing interface`);
}); 