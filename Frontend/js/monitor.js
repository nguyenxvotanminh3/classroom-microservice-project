// Circuit Breaker Monitor State
const monitorState = {
    circuitBreakers: new Map(),
    selectedBreaker: null,
    updateInterval: null,
    isUpdating: false
};

// Create initial grid container
function createGridContainer() {
    const container = document.getElementById('circuitBreakerList');
    if (!container) return;
    
    // Check if grid already exists
    if (document.getElementById('circuitBreakerGrid')) {
        return; // Don't recreate if it already exists
    }
    
    // Clear existing content
    container.innerHTML = '';
    
    // Create grid row with proper styling
    const gridRow = document.createElement('div');
    gridRow.className = 'row';
    gridRow.id = 'circuitBreakerGrid';
    gridRow.style.display = 'flex';
    gridRow.style.flexWrap = 'wrap';
    gridRow.style.margin = '0 -0.75rem';
    
    container.appendChild(gridRow);
}

// Update specific circuit breaker card
function updateCircuitBreakerCard(breakerData) {
    const { name, state, metrics } = breakerData;
    const cardId = `cb-card-${name}`;
    
    // Ensure grid container exists
    createGridContainer();
    const gridContainer = document.getElementById('circuitBreakerGrid');
    if (!gridContainer) return;
    
    let cardContainer = document.getElementById(cardId);
    
    // Create new card container if it doesn't exist
    if (!cardContainer) {
        cardContainer = document.createElement('div');
        cardContainer.id = cardId;
        cardContainer.className = 'col-xl-3 col-lg-4 col-md-6 mb-3';
        gridContainer.appendChild(cardContainer);
    }
    
    // Get the appropriate state class and color
    const stateColors = {
        CLOSED: { bg: 'bg-success', text: 'success' },
        OPEN: { bg: 'bg-danger', text: 'danger' },
        HALF_OPEN: { bg: 'bg-warning', text: 'warning' },
        DISABLED: { bg: 'bg-secondary', text: 'secondary' },
        METRICS_ONLY: { bg: 'bg-info', text: 'info' },
        FORCED_OPEN: { bg: 'bg-danger', text: 'danger' }
    };
    
    const stateColor = stateColors[state] || stateColors.DISABLED;
    
    // Use a simplified, fixed layout with inline styles for maximum compatibility
    cardContainer.innerHTML = `
        <div style="border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); background: white; overflow: hidden; height: 100%; cursor: pointer;" 
             class="${monitorState.selectedBreaker === name ? 'border border-primary border-2' : ''}">
            <!-- Card Header -->
            <div style="padding: 10px 15px; display: flex; justify-content: space-between; align-items: center; background-color: #f8f9fa; border-bottom: 1px solid #eee;">
                <div style="font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 70%;">${name}</div>
                <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; background-color: ${stateColor.bg.replace('bg-', '')}; color: white;">${state}</div>
            </div>
            
            <!-- Card Body -->
            <div style="padding: 15px;">
                <!-- Failure Rate Display -->
                <div style="margin-bottom: 15px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                        <div>Failure Rate:</div>
                        <div style="font-weight: 600; color: ${metrics.failureRate > 50 ? 'red' : (metrics.failureRate > 25 ? 'orange' : 'green')};">
                            ${metrics.failureRate.toFixed(1)}%
                        </div>
                    </div>
                    <div style="height: 6px; background-color: #e9ecef; border-radius: 3px; overflow: hidden;">
                        <div style="height: 100%; width: ${metrics.failureRate}%; background-color: ${metrics.failureRate > 50 ? 'red' : (metrics.failureRate > 25 ? 'orange' : 'green')};"></div>
                    </div>
                </div>
                
                <!-- Call Stats -->
                <div style="display: flex; align-items: center; font-size: 14px;">
                    <div style="margin-right: 5px;">Calls:</div>
                    <div style="color: green; font-weight: 600;">${metrics.numberOfSuccessfulCalls}</div>
                    <div style="margin: 0 5px;">/</div>
                    <div style="color: red; font-weight: 600;">${metrics.numberOfFailedCalls}</div>
                </div>
            </div>
        </div>
    `;
    
    // Add click handler
    const card = cardContainer.querySelector('div');
    card.addEventListener('click', () => {
        // Remove active class from all cards
        document.querySelectorAll('.border-primary').forEach(c => c.classList.remove('border', 'border-primary', 'border-2'));
        // Add active class to clicked card
        card.classList.add('border', 'border-primary', 'border-2');
        // Update details
        monitorState.selectedBreaker = name;
        updateCircuitBreakerDetails(breakerData);
    });
    
    // Update state map
    monitorState.circuitBreakers.set(name, breakerData);
}

// Update circuit breaker details panel
function updateCircuitBreakerDetails(breakerData) {
    const detailsContainer = document.getElementById('circuitBreakerDetails');
    if (!breakerData) {
        detailsContainer.innerHTML = '<p class="text-center text-muted">Select a circuit breaker to view details</p>';
        return;
    }

    const { name, state, metrics, config } = breakerData;
    const stateClass = getStateColorClass(state).replace('bg-', 'text-');
    
    detailsContainer.innerHTML = `
        <div class="row">
            <div class="col-12 mb-3">
                <h4 class="d-flex align-items-center">
                    <span class="me-2">${name}</span>
                    <span class="badge ${getStateColorClass(state)}">${state}</span>
                </h4>
            </div>
            <div class="col-md-6">
                <div class="card shadow-sm">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0"><i class="fas fa-chart-line me-2"></i>Metrics</h5>
                    </div>
                    <div class="card-body">
                        <ul class="list-group list-group-flush">
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Failure Rate
                                <span class="badge bg-danger">${metrics.failureRate.toFixed(1)}%</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Slow Call Rate
                                <span class="badge bg-warning">${metrics.slowCallRate.toFixed(1)}%</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Successful Calls
                                <span class="badge bg-success">${metrics.numberOfSuccessfulCalls}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Failed Calls
                                <span class="badge bg-danger">${metrics.numberOfFailedCalls}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Slow Calls
                                <span class="badge bg-warning">${metrics.numberOfSlowCalls}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Not Permitted Calls
                                <span class="badge bg-secondary">${metrics.numberOfNotPermittedCalls}</span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card shadow-sm">
                    <div class="card-header bg-info text-white">
                        <h5 class="mb-0"><i class="fas fa-cog me-2"></i>Configuration</h5>
                    </div>
                    <div class="card-body">
                        <ul class="list-group list-group-flush">
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Failure Rate Threshold
                                <span class="badge bg-primary">${config.failureRateThreshold}%</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Slow Call Rate Threshold
                                <span class="badge bg-primary">${config.slowCallRateThreshold}%</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Permitted Calls in Half-Open
                                <span class="badge bg-primary">${config.permittedNumberOfCallsInHalfOpenState}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Sliding Window Size
                                <span class="badge bg-primary">${config.slidingWindowSize}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                Minimum Calls
                                <span class="badge bg-primary">${config.minimumNumberOfCalls}</span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// Get color class based on circuit breaker state
function getStateColorClass(state) {
    const stateColors = {
        CLOSED: 'bg-success text-white',
        OPEN: 'bg-danger text-white',
        HALF_OPEN: 'bg-warning text-dark',
        DISABLED: 'bg-secondary text-white',
        METRICS_ONLY: 'bg-info text-white',
        FORCED_OPEN: 'bg-danger text-white'
    };
    return stateColors[state] || 'bg-secondary text-white';
}

// Start monitoring updates
function startMonitoring(intervalMs = 5000) {
    // Initial update
    updateAllCircuitBreakers();
    
    // Set up interval for updates
    if (monitorState.updateInterval) {
        clearInterval(monitorState.updateInterval);
    }
    monitorState.updateInterval = setInterval(updateAllCircuitBreakers, intervalMs);
}

// Stop monitoring updates
function stopMonitoring() {
    if (monitorState.updateInterval) {
        clearInterval(monitorState.updateInterval);
        monitorState.updateInterval = null;
    }
}

// Show loading spinner
function showLoading() {
    const refreshButton = document.getElementById('refreshStatus');
    if (refreshButton) {
        refreshButton.disabled = true;
        refreshButton.innerHTML = `
            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            Updating...
        `;
    }
}

// Hide loading spinner
function hideLoading() {
    const refreshButton = document.getElementById('refreshStatus');
    if (refreshButton) {
        refreshButton.disabled = false;
        refreshButton.innerHTML = `
            <i class="fas fa-sync-alt me-1"></i>Refresh
        `;
    }
}

// Update all circuit breakers
async function updateAllCircuitBreakers() {
    if (monitorState.isUpdating) return;
    
    try {
        monitorState.isUpdating = true;
        showLoading();
        
        const response = await API.monitor.getAll();
        if (response.circuitBreakers) {
            // Clear removed circuit breakers
            const currentBreakers = new Set(response.circuitBreakers.map(cb => cb.name));
            for (const [name] of monitorState.circuitBreakers) {
                if (!currentBreakers.has(name)) {
                    const cardElement = document.getElementById(`cb-card-${name}`);
                    if (cardElement) {
                        cardElement.remove();
                    }
                    monitorState.circuitBreakers.delete(name);
                }
            }
            
            // Update existing and add new circuit breakers
            response.circuitBreakers.forEach(breaker => {
                updateCircuitBreakerCard(breaker);
            });
            
            // Update details if a breaker is selected
            if (monitorState.selectedBreaker) {
                const selectedBreakerData = response.circuitBreakers.find(
                    cb => cb.name === monitorState.selectedBreaker
                );
                updateCircuitBreakerDetails(selectedBreakerData);
            }
        }
    } catch (error) {
        console.error('Failed to update circuit breakers:', error);
    } finally {
        monitorState.isUpdating = false;
        hideLoading();
    }
}

// Initialize monitor
document.addEventListener('DOMContentLoaded', () => {
    // Add custom styles directly to document head to ensure they're always available
    const style = document.createElement('style');
    style.textContent = `
        .hover-card {
            transition: all 0.3s ease;
            cursor: pointer;
        }
        .hover-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important;
        }
        .active-card {
            transform: translateY(-2px);
            box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important;
            border: 2px solid #0d6efd;
        }
        .card {
            position: relative;
            border: none;
            border-radius: 0.75rem;
            box-shadow: 0 0.125rem 0.25rem rgba(0,0,0,0.075);
            background: #fff;
            overflow: hidden;
        }
        .circuit-breaker-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 1.5rem;
            padding: 1.5rem;
        }
    `;
    document.head.appendChild(style);
    
    // Start monitoring with 5 second interval
    startMonitoring(5000);
    
    // Setup all refresh buttons (there might be multiple with the same ID)
    document.querySelectorAll('#refreshStatus').forEach(refreshButton => {
        if (refreshButton) {
            // Remove any existing event listeners
            refreshButton.replaceWith(refreshButton.cloneNode(true));
            
            // Get the new button and add event listener
            const newButton = document.getElementById(refreshButton.id);
            newButton.addEventListener('click', function(e) {
                e.preventDefault(); // Prevent any form submission
                updateAllCircuitBreakers();
            });
        }
    });
});

// Export monitor functions
window.Monitor = {
    start: startMonitoring,
    stop: stopMonitoring,
    update: updateAllCircuitBreakers
}; 