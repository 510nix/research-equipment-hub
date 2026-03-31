/**
 * Research Equipment Hub - Application JavaScript
 * Handles DOM interactions, form validations, and AJAX requests
 * 
 * [STABILITY FIX]: Prevents JSON parsing of HTML content
 * [STABILITY FIX]: Avoids external CDN dependencies
 */

console.log('[APP] Research Equipment Hub initialized');

/**
 * Utility: Safe JSON parse with fallback
 * Prevents the "Invalid JSON" error when trying to parse HTML content
 */
function safeJsonParse(jsonString) {
    try {
        // Check if the string looks like JSON (starts with '{' or '[')
        if (!jsonString || typeof jsonString !== 'string') {
            console.warn('[APP] Invalid JSON input type:', typeof jsonString);
            return null;
        }
        
        const trimmed = jsonString.trim();
        if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
            console.warn('[APP] Content does not appear to be JSON. Skipping parse.');
            return null;
        }
        
        return JSON.parse(jsonString);
    } catch (error) {
        console.error('[APP] JSON parsing failed:', error.message);
        console.error('[APP] Attempted to parse:', jsonString.substring(0, 100));
        return null;
    }
}

/**
 * DOM Ready Handler
 * Initialize components once DOM is loaded
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('[APP] DOM loaded. Initializing components...');
    
    // Initialize Bootstrap tooltips and popovers if Bootstrap JS is available
    initBootstrapComponents();
    
    // Setup form validation
    setupFormValidation();
    
    // Setup AJAX error handling
    setupAjaxErrorHandling();
});

/**
 * Initialize Bootstrap components
 * Gracefully handles Bootstrap unavailability
 */
function initBootstrapComponents() {
    try {
        // Check if Bootstrap bundle is available
        if (typeof bootstrap !== 'undefined') {
            console.log('[APP] Bootstrap library detected. Initializing components...');
            
            // Initialize all Bootstrap tooltips
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function(tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
            
            console.log('[APP] Bootstrap components initialized successfully');
        } else {
            console.warn('[APP] Bootstrap library not available. Skipping component initialization.');
        }
    } catch (error) {
        console.error('[APP] Error initializing Bootstrap components:', error.message);
    }
}

/**
 * Setup form validation
 * Prevent form submission with validation
 */
function setupFormValidation() {
    const forms = document.querySelectorAll('form[novalidate]');
    
    forms.forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                console.warn('[APP] Form validation failed');
            }
            form.classList.add('was-validated');
        }, false);
    });
}

/**
 * Setup AJAX error handling
 * Handle errors from fetch/AJAX requests gracefully
 */
function setupAjaxErrorHandling() {
    // Override fetch to add error handling
    const originalFetch = window.fetch;
    window.fetch = function(...args) {
        return originalFetch.apply(this, args)
            .catch(function(error) {
                console.error('[APP] Fetch error:', error.message);
                showErrorNotification('Network request failed. Please try again.');
                throw error;
            });
    };
}

/**
 * Display error notification to user
 */
function showErrorNotification(message) {
    try {
        // Create bootstrap alert if Bootstrap is available
        if (typeof bootstrap !== 'undefined') {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger alert-dismissible fade show';
            alertDiv.role = 'alert';
            alertDiv.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            
            const container = document.querySelector('body');
            if (container) {
                container.insertBefore(alertDiv, container.firstChild);
                console.log('[APP] Error notification displayed');
            }
        } else {
            // Fallback: Use browser alert
            alert(message);
        }
    } catch (error) {
        console.error('[APP] Error displaying notification:', error);
    }
}

/**
 * [STABILITY FIX] Safe JSON response parsing
 * Prevents "Unexpected token" errors when fetch returns HTML instead of JSON
 * This occurs when redirects happen (401 Unauthorized = login page HTML)
 */
function parseJsonResponse(response) {
    const contentType = response.headers.get('content-type');
    
    // Check if response claims to be JSON
    if (!contentType || !contentType.includes('application/json')) {
        console.warn('[APP] Response is not JSON. Content-Type:', contentType);
        console.warn('[APP] Response status:', response.status);
        console.warn('[APP] This usually means a redirect occurred (e.g., 401 to login)');
        return null;
    }
    
    return response.text().then(text => {
        try {
            if (!text) {
                console.warn('[APP] Empty response body');
                return null;
            }
            return safeJsonParse(text);
        } catch (error) {
            console.error('[APP] Failed to parse response body:', error);
            return null;
        }
    });
}

/**
 * Utility: Make AJAX request with error handling
 * [STABILITY FIX] Protected from JSON parsing errors on redirects
 */
function makeRequest(url, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    console.log('[APP] Making request to:', url, 'with method:', finalOptions.method);
    
    return fetch(url, finalOptions)
        .then(response => {
            // Log the response for debugging
            console.log('[APP] Response received:', {
                status: response.status,
                statusText: response.statusText,
                contentType: response.headers.get('content-type')
            });
            
            // Check for redirect responses that return HTML
            if (response.status === 401 || response.status === 403) {
                console.warn('[APP] Authentication/Authorization issue detected. May redirect to login.');
                throw new Error(`HTTP ${response.status}: ${response.statusText}. Check browser navigation.`);
            }
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return response;
        })
        .catch(error => {
            console.error('[APP] Request failed:', error.message);
            console.error('[APP] Full error:', error);
            throw error;
        });
}

/**
 * Utility: Make JSON API request (with response validation)
 */
function makeJsonRequest(url, options = {}) {
    return makeRequest(url, options)
        .then(response => parseJsonResponse(response))
        .catch(error => {
            console.error('[APP] JSON request failed:', error);
            throw error;
        });
}

// Export functions globally for inline HTML event handlers
window.app = {
    makeRequest,
    makeJsonRequest,
    safeJsonParse,
    parseJsonResponse,
    showErrorNotification
};

console.log('[APP] Application ready with protected JSON parsing');
