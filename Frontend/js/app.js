/**
 * Circuit Breaker Dashboard Application
 */

// Global state
const state = {
    currentCircuitBreaker: null,
    refreshInterval: null,
    autoRefreshEnabled: true,
    refreshRate: 5000, // ms
    testKeyMemory: null // To store memory test key for release
};

// DOM elements - with safe initialization
const elements = {
    // Initialize with empty object first
};

/**
 * Safely get DOM elements
 */
function initDOMElements() {
    // Circuit breaker status elements
    elements.circuitBreakerList = document.getElementById('circuitBreakerList');
    elements.circuitBreakerDetails = document.getElementById('circuitBreakerDetails');
    elements.refreshStatus = document.querySelectorAll('#refreshStatus');
    
    // Circuit breaker control elements
    elements.circuitBreakerName = document.getElementById('circuitBreakerName');
    elements.circuitBreakerState = document.getElementById('circuitBreakerState');
    elements.forceState = document.getElementById('forceState');
    elements.resetMetrics = document.getElementById('resetMetrics');
    elements.controlResults = document.getElementById('controlResults');
    elements.forceStateForm = document.getElementById('forceStateForm');
    
    // Stress test elements
    elements.stressTestForm = document.getElementById('stressTestForm');
    elements.concurrentRequests = document.getElementById('concurrentRequests');
    elements.includeDuplicates = document.getElementById('includeDuplicates');
    elements.runStressTest = document.getElementById('runStressTest');
    elements.stressTestResults = document.getElementById('stressTestResults');
    
    // Slow request elements
    elements.slowRequestForm = document.getElementById('slowRequestForm');
    elements.delaySeconds = document.getElementById('delaySeconds');
    elements.runSlowRequest = document.getElementById('runSlowRequest');
    elements.slowRequestResults = document.getElementById('slowRequestResults');
    
    // Connection pool elements
    elements.connectionPoolForm = document.getElementById('connectionPoolForm');
    elements.requests = document.getElementById('requests');
    elements.sleepMillis = document.getElementById('sleepMillis');
    elements.runConnectionPool = document.getElementById('runConnectionPool');
    elements.connectionPoolResults = document.getElementById('connectionPoolResults');
    
    // Memory pressure elements
    elements.memoryPressureForm = document.getElementById('memoryPressureForm');
    elements.mbToAllocate = document.getElementById('mbToAllocate');
    elements.holdForSeconds = document.getElementById('holdForSeconds');
    elements.runMemoryPressure = document.getElementById('runMemoryPressure');
    elements.memoryPressureResults = document.getElementById('memoryPressureResults');
    elements.releaseAllMemory = document.getElementById('releaseAllMemory');
    
    console.log("DOM Elements initialized:", elements);
}

/**
 * Initialize the application
 */
function init() {
    console.log("Initializing application...");
    
    // Initialize DOM elements first
    initDOMElements();
    
    // Load circuit breaker names if the element exists
    if (elements.circuitBreakerName) {
        loadCircuitBreakerNames();
    } else {
        console.warn("Circuit breaker name selector not found in the DOM");
    }
    
    // Start auto refresh
    startAutoRefresh();
    
    // Setup event listeners
    setupEventListeners();
    
    console.log("Application initialized");
}

/**
 * Setup all event listeners
 */
function setupEventListeners() {
    console.log("Setting up event listeners...");
    
    // Refresh status button (might be multiple with same ID)
    if (elements.refreshStatus && elements.refreshStatus.length > 0) {
        elements.refreshStatus.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                console.log("Refresh button clicked");
                // First update using the monitor module if available
                if (window.Monitor && typeof window.Monitor.update === 'function') {
                    window.Monitor.update();
                }
                // Then use our own function
                loadAllCircuitBreakers();
            });
        });
    } else {
        console.warn("No refresh buttons found. Using element ID 'refreshStatus'");
        // Try to get by ID directly as a fallback
        const refreshButtons = document.querySelectorAll('#refreshStatus');
        refreshButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                console.log("Refresh button clicked (fallback)");
                // First update using the monitor module if available
                if (window.Monitor && typeof window.Monitor.update === 'function') {
                    window.Monitor.update();
                }
                // Then use our own function
                loadAllCircuitBreakers();
            });
        });
    }
    
    // Force state form
    if (elements.forceStateForm) {
        elements.forceStateForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (elements.circuitBreakerName && elements.circuitBreakerState) {
                const name = elements.circuitBreakerName.value;
                const state = elements.circuitBreakerState.value;
                
                if (name) {
                    forceCircuitBreakerState(name, state);
                }
            }
        });
    }
    
    // Reset metrics button
    if (elements.resetMetrics) {
        elements.resetMetrics.addEventListener('click', () => {
            if (elements.circuitBreakerName) {
                const name = elements.circuitBreakerName.value;
                if (name) {
                    resetCircuitBreakerMetrics(name);
                }
            }
        });
    }
    
    // Stress test form
    if (elements.stressTestForm) {
        elements.stressTestForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (elements.concurrentRequests) {
                const concurrentRequests = elements.concurrentRequests.value;
                const includeDuplicates = elements.includeDuplicates ? elements.includeDuplicates.checked : false;
                
                runStressTest(concurrentRequests, includeDuplicates);
            }
        });
    }
    
    // Slow request form
    if (elements.slowRequestForm) {
        elements.slowRequestForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (elements.delaySeconds) {
                const delaySeconds = elements.delaySeconds.value;
                runSlowRequest(delaySeconds);
            }
        });
    }
    
    // Connection pool form
    if (elements.connectionPoolForm) {
        elements.connectionPoolForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (elements.requests && elements.sleepMillis) {
                const requests = elements.requests.value;
                const sleepMillis = elements.sleepMillis.value;
                
                runConnectionPoolTest(requests, sleepMillis);
            }
        });
    }
    
    // Memory pressure form
    if (elements.memoryPressureForm) {
        elements.memoryPressureForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (elements.mbToAllocate && elements.holdForSeconds) {
                const mbToAllocate = elements.mbToAllocate.value;
                const holdForSeconds = elements.holdForSeconds.value;
                
                runMemoryPressureTest(mbToAllocate, holdForSeconds);
            }
        });
    }
    
    // Release all memory button
    if (elements.releaseAllMemory) {
        elements.releaseAllMemory.addEventListener('click', () => {
            releaseAllMemory();
        });
    }
    
    console.log("Event listeners setup complete");
}

/**
 * Start auto refresh of circuit breaker status
 */
function startAutoRefresh() {
    if (state.refreshInterval) {
        clearInterval(state.refreshInterval);
    }
    
    state.refreshInterval = setInterval(() => {
        if (state.autoRefreshEnabled) {
            loadAllCircuitBreakers();
        }
    }, state.refreshRate);
}

/**
 * Load all circuit breaker names
 */
async function loadCircuitBreakerNames() {
    try {
        const response = await API.monitor.getNames();
        
        if (response.error) {
            showError('Failed to load circuit breaker names', response.details);
            return;
        }
        
        if (response.circuitBreakers && Array.isArray(response.circuitBreakers) && elements.circuitBreakerName) {
            // Populate the dropdown
            const select = elements.circuitBreakerName;
            select.innerHTML = '';
            
            response.circuitBreakers.forEach(name => {
                const option = document.createElement('option');
                option.value = name;
                option.textContent = name;
                select.appendChild(option);
            });
            
            // If we have circuit breakers, select the first one
            if (response.circuitBreakers.length > 0) {
                select.value = response.circuitBreakers[0];
                loadCircuitBreakerDetails(response.circuitBreakers[0]);
            }
        } else {
            console.warn("Error loading circuit breaker names: select is null");
        }
        
        // Also load all circuit breakers
        loadAllCircuitBreakers();
        
    } catch (error) {
        showError('Error loading circuit breaker names', error.message);
    }
}

/**
 * Load all circuit breakers
 */
async function loadAllCircuitBreakers() {
    try {
        if (!elements.circuitBreakerList) {
            console.warn("Circuit breaker list element not found");
            return;
        }
        
        elements.circuitBreakerList.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `;
        
        const response = await API.monitor.getAll();
        
        if (response.error) {
            showError('Failed to load circuit breakers', response.details);
            return;
        }
        
        if (response.circuitBreakers && Array.isArray(response.circuitBreakers)) {
            renderCircuitBreakerList(response.circuitBreakers);
        }
        
    } catch (error) {
        showError('Error loading circuit breakers', error.message);
    }
}

/**
 * Render the circuit breaker list
 */
function renderCircuitBreakerList(circuitBreakers) {
    if (!elements.circuitBreakerList) {
        console.warn("Circuit breaker list element not found");
        return;
    }
    
    if (circuitBreakers.length === 0) {
        elements.circuitBreakerList.innerHTML = `
            <div class="alert alert-info">
                No circuit breakers found.
            </div>
        `;
        return;
    }
    
    // Create a row container for flex layout
    const rowContainer = document.createElement('div');
    rowContainer.style.display = 'flex';
    rowContainer.style.flexWrap = 'wrap';
    rowContainer.style.margin = '0 -0.75rem';
    
    // Generate cards with clean inline styles
    circuitBreakers.forEach(cb => {
        const state = cb.state;
        const metrics = cb.metrics;
        const failureRate = metrics.failureRate || 0;
        
        // Get color based on state
        const stateColors = {
            CLOSED: { bg: '#28a745', text: 'white' },
            OPEN: { bg: '#dc3545', text: 'white' },
            HALF_OPEN: { bg: '#ffc107', text: 'black' },
            DISABLED: { bg: '#6c757d', text: 'white' },
            METRICS_ONLY: { bg: '#17a2b8', text: 'white' },
            FORCED_OPEN: { bg: '#dc3545', text: 'white' }
        };
        
        const stateColor = stateColors[state] || stateColors.DISABLED;
        const failureRateColor = failureRate > 50 ? '#dc3545' : (failureRate > 25 ? '#ffc107' : '#28a745');
        
        // Create card container
        const cardContainer = document.createElement('div');
        cardContainer.className = 'circuit-breaker-card';
        cardContainer.style.width = '25%';
        cardContainer.style.padding = '0.75rem';
        cardContainer.style.minWidth = '300px';
        cardContainer.style.maxWidth = '100%';
        cardContainer.setAttribute('data-cb-name', cb.name);
        
        // Card HTML with clean inline styles
        cardContainer.innerHTML = `
            <div style="border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); background: white; overflow: hidden; height: 100%; cursor: pointer;">
                <!-- Card Header -->
                <div style="padding: 10px 15px; display: flex; justify-content: space-between; align-items: center; background-color: #f8f9fa; border-bottom: 1px solid #eee;">
                    <div style="font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 70%;">${cb.name}</div>
                    <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; background-color: ${stateColor.bg}; color: ${stateColor.text};">${state}</div>
                </div>
                
                <!-- Card Body -->
                <div style="padding: 15px;">
                    <!-- Failure Rate Display -->
                    <div style="margin-bottom: 15px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <div>Failure Rate:</div>
                            <div style="font-weight: 600; color: ${failureRateColor};">
                                ${failureRate.toFixed(1)}%
                            </div>
                        </div>
                        <div style="height: 6px; background-color: #e9ecef; border-radius: 3px; overflow: hidden;">
                            <div style="height: 100%; width: ${failureRate}%; background-color: ${failureRateColor};"></div>
                        </div>
                    </div>
                    
                    <!-- Call Stats -->
                    <div style="display: flex; align-items: center; font-size: 14px;">
                        <div style="margin-right: 5px;">Calls:</div>
                        <div style="color: #28a745; font-weight: 600;">${metrics.numberOfSuccessfulCalls}</div>
                        <div style="margin: 0 5px;">/</div>
                        <div style="color: #dc3545; font-weight: 600;">${metrics.numberOfFailedCalls}</div>
                        ${metrics.numberOfNotPermittedCalls > 0 ? 
                            `<div style="margin: 0 5px;">/</div>
                            <div style="color: #ffc107; font-weight: 600;">${metrics.numberOfNotPermittedCalls} blocked</div>` : 
                            ''}
                    </div>
                </div>
            </div>
        `;
        
        rowContainer.appendChild(cardContainer);
    });
    
    // Replace content
    elements.circuitBreakerList.innerHTML = '';
    elements.circuitBreakerList.appendChild(rowContainer);
    
    // Add click event to each card
    document.querySelectorAll('.circuit-breaker-card').forEach(card => {
        card.addEventListener('click', () => {
            const name = card.getAttribute('data-cb-name');
            if (name) {
                loadCircuitBreakerDetails(name);
                
                // Update the selected class
                document.querySelectorAll('.circuit-breaker-card > div').forEach(c => 
                    c.style.border = 'none');
                card.querySelector('div').style.border = '2px solid #007bff';
                
                // Update the dropdown if it exists
                if (elements.circuitBreakerName) {
                    elements.circuitBreakerName.value = name;
                }
            }
        });
    });
}

/**
 * Load circuit breaker details
 */
async function loadCircuitBreakerDetails(name) {
    try {
        elements.circuitBreakerDetails.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `;
        
        const response = await API.monitor.getByName(name);
        
        if (response.error) {
            showError(`Failed to load details for ${name}`, response.details);
            return;
        }
        
        renderCircuitBreakerDetails(response);
        
    } catch (error) {
        showError(`Error loading details for ${name}`, error.message);
    }
}

/**
 * Render circuit breaker details
 */
function renderCircuitBreakerDetails(cb) {
    const state = cb.state;
    const metrics = cb.metrics;
    const config = cb.config;
    
    const html = `
        <h4 class="mb-3">${cb.name}</h4>
        
        <div class="row">
            <div class="col-md-6">
                <div class="card mb-3">
                    <div class="card-header bg-secondary text-white">
                        State Information
                    </div>
                    <div class="card-body">
                        <div class="metric-row">
                            <span class="metric-label">Current State:</span>
                            <span class="cb-badge cb-${state.toLowerCase()}">${state}</span>
                        </div>
                    </div>
                </div>
                
                <div class="card mb-3">
                    <div class="card-header bg-primary text-white">
                        Metrics
                    </div>
                    <div class="card-body">
                        <div class="metric-row">
                            <span class="metric-label">Failure Rate:</span>
                            <span class="metric-value">${metrics.failureRate.toFixed(1)}%</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Slow Call Rate:</span>
                            <span class="metric-value">${metrics.slowCallRate.toFixed(1)}%</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Successful Calls:</span>
                            <span class="metric-value text-success">${metrics.numberOfSuccessfulCalls}</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Failed Calls:</span>
                            <span class="metric-value text-danger">${metrics.numberOfFailedCalls}</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Slow Calls:</span>
                            <span class="metric-value text-warning">${metrics.numberOfSlowCalls}</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Not Permitted Calls:</span>
                            <span class="metric-value text-muted">${metrics.numberOfNotPermittedCalls}</span>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-md-6">
                <div class="card mb-3">
                    <div class="card-header bg-info text-white">
                        Configuration
                    </div>
                    <div class="card-body">
                        <div class="metric-row">
                            <span class="metric-label">Failure Rate Threshold:</span>
                            <span class="metric-value">${config.failureRateThreshold}%</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Slow Call Rate Threshold:</span>
                            <span class="metric-value">${config.slowCallRateThreshold}%</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Min. Calls for Calculation:</span>
                            <span class="metric-value">${config.minimumNumberOfCalls}</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Sliding Window Size:</span>
                            <span class="metric-value">${config.slidingWindowSize}</span>
                        </div>
                        <div class="metric-row">
                            <span class="metric-label">Permitted Calls in Half-Open:</span>
                            <span class="metric-value">${config.permittedNumberOfCallsInHalfOpenState}</span>
                        </div>
                    </div>
                </div>
                
                <div class="card mb-3">
                    <div class="card-header bg-success text-white">
                        Configuration Details
                    </div>
                    <div class="card-body">
                        <pre style="max-height: 200px; overflow-y: auto;">${config.configDetails || 'No detailed configuration available.'}</pre>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    elements.circuitBreakerDetails.innerHTML = html;
}

/**
 * Force circuit breaker state
 */
async function forceCircuitBreakerState(name, state) {
    try {
        elements.controlResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `;
        
        const response = await API.monitor.forceState(name, state);
        
        if (response.error) {
            showError(`Failed to force state for ${name}`, response.details);
            return;
        }
        
        elements.controlResults.innerHTML = `
            <div class="alert alert-success">
                Successfully changed state of ${name} to ${response.newState}
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Refresh the circuit breaker list
        loadAllCircuitBreakers();
        
        // Refresh the details for this circuit breaker
        loadCircuitBreakerDetails(name);
        
    } catch (error) {
        showError(`Error forcing state for ${name}`, error.message);
    }
}

/**
 * Reset circuit breaker metrics
 */
async function resetCircuitBreakerMetrics(name) {
    try {
        elements.controlResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `;
        
        const response = await API.monitor.reset(name);
        
        if (response.error) {
            showError(`Failed to reset metrics for ${name}`, response.details);
            return;
        }
        
        elements.controlResults.innerHTML = `
            <div class="alert alert-success">
                Successfully reset metrics for ${name}
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Refresh the circuit breaker list
        loadAllCircuitBreakers();
        
        // Refresh the details for this circuit breaker
        loadCircuitBreakerDetails(name);
        
    } catch (error) {
        showError(`Error resetting metrics for ${name}`, error.message);
    }
}

/**
 * Run a stress test
 */
async function runStressTest(concurrentRequests, includeDuplicates) {
    try {
        elements.stressTestResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">This may take some time...</div>
        `;
        
        // Disable the run button
        elements.runStressTest.disabled = true;
        
        const response = await API.stressTest.runStressTest(concurrentRequests, includeDuplicates);
        
        if (response.error) {
            showError('Failed to run stress test', response.details);
            elements.runStressTest.disabled = false;
            return;
        }
        
        elements.stressTestResults.innerHTML = `
            <div class="alert ${response.circuitBreakerTriggered ? 'alert-warning' : 'alert-success'}">
                ${response.circuitBreakerTriggered 
                    ? `Circuit breaker triggered! ${response.failed} requests failed.` 
                    : 'Stress test completed successfully.'}
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
        // Re-enable the run button
        elements.runStressTest.disabled = false;
        
    } catch (error) {
        showError('Error running stress test', error.message);
        elements.runStressTest.disabled = false;
    }
}

/**
 * Run a slow request test
 */
async function runSlowRequest(delaySeconds) {
    try {
        elements.slowRequestResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">Waiting for ${delaySeconds} seconds...</div>
        `;
        
        // Disable the run button
        elements.runSlowRequest.disabled = true;
        
        const response = await API.stressTest.runSlowRequest(delaySeconds);
        
        if (response.error) {
            elements.slowRequestResults.innerHTML = `
                <div class="alert alert-danger">
                    Failed to complete slow request: ${response.message}
                </div>
                <pre>${JSON.stringify(response, null, 2)}</pre>
            `;
            elements.runSlowRequest.disabled = false;
            
            // Refresh the circuit breaker information
            loadAllCircuitBreakers();
            return;
        }
        
        elements.slowRequestResults.innerHTML = `
            <div class="alert alert-success">
                Slow request completed successfully after ${delaySeconds} seconds delay.
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
        // Re-enable the run button
        elements.runSlowRequest.disabled = false;
        
    } catch (error) {
        showError('Error running slow request', error.message);
        elements.runSlowRequest.disabled = false;
    }
}

/**
 * Run a connection pool test
 */
async function runConnectionPoolTest(requests, sleepMillis) {
    try {
        elements.connectionPoolResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">This may take some time...</div>
        `;
        
        // Disable the run button
        elements.runConnectionPool.disabled = true;
        
        const response = await API.loadTest.runConnectionPool(requests, sleepMillis);
        
        if (response.error) {
            showError('Failed to run connection pool test', response.details);
            elements.runConnectionPool.disabled = false;
            return;
        }
        
        elements.connectionPoolResults.innerHTML = `
            <div class="alert alert-success">
                Connection pool test completed in ${response.totalExecutionTimeMs}ms
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
        // Re-enable the run button
        elements.runConnectionPool.disabled = false;
        
    } catch (error) {
        showError('Error running connection pool test', error.message);
        elements.runConnectionPool.disabled = false;
    }
}

/**
 * Run a memory pressure test
 */
async function runMemoryPressureTest(mbToAllocate, holdForSeconds) {
    try {
        elements.memoryPressureResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">Allocating ${mbToAllocate}MB of memory...</div>
        `;
        
        // Disable the run button
        elements.runMemoryPressure.disabled = true;
        
        const response = await API.loadTest.runMemoryPressure(mbToAllocate, holdForSeconds);
        
        if (response.error) {
            showError('Failed to run memory pressure test', response.details);
            elements.runMemoryPressure.disabled = false;
            return;
        }
        
        // Store the test key for future release
        state.testKeyMemory = response.testKey;
        
        elements.memoryPressureResults.innerHTML = `
            <div class="alert alert-warning">
                Memory pressure test active: ${response.allocatedMB}MB allocated, will be released in ${response.holdForSeconds} seconds
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
            <button id="releaseMemoryTest" class="btn btn-sm btn-outline-danger mt-2">
                <i class="fas fa-trash-alt me-1"></i>Release This Memory Test
            </button>
        `;
        
        // Add event listener to the release button
        document.getElementById('releaseMemoryTest').addEventListener('click', () => {
            if (state.testKeyMemory) {
                releaseMemory(state.testKeyMemory);
            }
        });
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
        // Re-enable the run button
        elements.runMemoryPressure.disabled = false;
        
    } catch (error) {
        showError('Error running memory pressure test', error.message);
        elements.runMemoryPressure.disabled = false;
    }
}

/**
 * Release memory from a specific test
 */
async function releaseMemory(testKey) {
    try {
        elements.memoryPressureResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">Releasing memory...</div>
        `;
        
        const response = await API.loadTest.releaseMemory(testKey);
        
        if (response.error) {
            showError('Failed to release memory', response.details);
            return;
        }
        
        elements.memoryPressureResults.innerHTML = `
            <div class="alert alert-success">
                Memory successfully released for test ${testKey.substring(0, 8)}...
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Clear the test key
        state.testKeyMemory = null;
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
    } catch (error) {
        showError('Error releasing memory', error.message);
    }
}

/**
 * Release all memory
 */
async function releaseAllMemory() {
    try {
        elements.memoryPressureResults.innerHTML = `
            <div class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
            <div class="text-center mt-2">Releasing all memory...</div>
        `;
        
        const response = await API.loadTest.releaseAllMemory();
        
        if (response.error) {
            showError('Failed to release all memory', response.details);
            return;
        }
        
        elements.memoryPressureResults.innerHTML = `
            <div class="alert alert-success">
                All memory successfully released (${response.releasedTests} tests)
            </div>
            <pre>${JSON.stringify(response, null, 2)}</pre>
        `;
        
        // Clear the test key
        state.testKeyMemory = null;
        
        // Refresh the circuit breaker information
        loadAllCircuitBreakers();
        
    } catch (error) {
        showError('Error releasing all memory', error.message);
    }
}

/**
 * Show an error message in the console and optionally in the UI
 */
function showError(message, details) {
    console.error(message, details);
    // Can show a UI notification if needed
}

// Initialize when the DOM is ready
document.addEventListener('DOMContentLoaded', init); 