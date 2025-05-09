/**
 * API Service for Circuit Breaker Dashboard
 */

// Base URL for API calls - adjust this as needed
const API_BASE_URL = 'http://localhost:8080/api';

// Circuit Breaker Monitor API
const API = {
    // Circuit Breaker Monitor APIs
    monitor: {
        getNames: async () => {
            return fetch(`${API_BASE_URL}/circuit-breaker-monitor/names`)
                .then(response => response.json())
                .catch(error => handleApiError('Failed to get circuit breaker names', error));
        },
        
        getAll: async () => {
            return fetch(`${API_BASE_URL}/circuit-breaker-monitor/all`)
                .then(response => response.json())
                .catch(error => handleApiError('Failed to get all circuit breakers', error));
        },
        
        getByName: async (name) => {
            return fetch(`${API_BASE_URL}/circuit-breaker-monitor/${name}`)
                .then(response => response.json())
                .catch(error => handleApiError(`Failed to get circuit breaker: ${name}`, error));
        },
        
        forceState: async (name, state) => {
            return fetch(`${API_BASE_URL}/circuit-breaker-monitor/${name}/force/${state}`)
                .then(response => response.json())
                .catch(error => handleApiError(`Failed to force state: ${state} for ${name}`, error));
        },
        
        reset: async (name) => {
            return fetch(`${API_BASE_URL}/circuit-breaker-monitor/${name}/reset`)
                .then(response => response.json())
                .catch(error => handleApiError(`Failed to reset metrics for ${name}`, error));
        }
    },
    
    // Stress Test APIs
    stressTest: {
        runStressTest: async (concurrentRequests, includeDuplicates) => {
            const url = `${API_BASE_URL}/circuit-breaker-test/stress-test?concurrentRequests=${concurrentRequests}&includeDuplicates=${includeDuplicates}`;
            return fetch(url, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to run stress test', error));
        },
        
        runSlowRequest: async (delaySeconds) => {
            const url = `${API_BASE_URL}/circuit-breaker-test/slow-requests?delaySeconds=${delaySeconds}`;
            return fetch(url, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to run slow request test', error));
        },
        
        resetCounters: async () => {
            return fetch(`${API_BASE_URL}/circuit-breaker-test/reset-counters`, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to reset counters', error));
        }
    },
    
    // Load Test APIs
    loadTest: {
        runConnectionPool: async (requests, sleepMillis) => {
            const url = `${API_BASE_URL}/circuit-breaker-load/connection-pool?requests=${requests}&sleepMillis=${sleepMillis}`;
            return fetch(url, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to run connection pool test', error));
        },
        
        runMemoryPressure: async (mbToAllocate, holdForSeconds) => {
            const url = `${API_BASE_URL}/circuit-breaker-load/memory-pressure?mbToAllocate=${mbToAllocate}&holdForSeconds=${holdForSeconds}`;
            return fetch(url, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to run memory pressure test', error));
        },
        
        releaseMemory: async (testKey) => {
            return fetch(`${API_BASE_URL}/circuit-breaker-load/release-memory/${testKey}`, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to release memory', error));
        },
        
        releaseAllMemory: async () => {
            return fetch(`${API_BASE_URL}/circuit-breaker-load/release-all-memory`, { method: 'POST' })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to release all memory', error));
        }
    },

    // K6 Performance Testing APIs
    k6Test: {
        runTest: async (script, env) => {
            const url = `${API_BASE_URL}/k6-test/run`;
            return fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    script,
                    env
                })
            })
                .then(response => response.json())
                .catch(error => handleApiError('Failed to run K6 test', error));
        },
        
        getStatus: async (testId) => {
            return fetch(`${API_BASE_URL}/k6-test/status/${testId}`)
                .then(response => response.json())
                .catch(error => handleApiError(`Failed to get status for test: ${testId}`, error));
        },
        
        getResults: async (testId) => {
            return fetch(`${API_BASE_URL}/k6-test/results/${testId}`)
                .then(response => response.json())
                .catch(error => handleApiError(`Failed to get results for test: ${testId}`, error));
        },
        
        listTests: async () => {
            return fetch(`${API_BASE_URL}/k6-test/list`)
                .then(response => response.json())
                .catch(error => handleApiError('Failed to list K6 tests', error));
        },
        
        getTemplates: async () => {
            return fetch(`${API_BASE_URL}/k6-test/templates`)
                .then(response => response.json())
                .catch(error => handleApiError('Failed to get K6 test templates', error));
        }
    }
};

/**
 * Handle API errors in a consistent way
 */
function handleApiError(message, error) {
    console.error(`${message}:`, error);
    return {
        error: true,
        message: message,
        details: error.message
    };
}

// Export the API service
window.API = API; 