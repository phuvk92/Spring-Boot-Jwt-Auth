import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Custom metrics for V1 and V2
export const v1Duration = new Trend('v1_preview_duration', true);
export const v2Duration = new Trend('v2_preview_duration', true);
export const v1Success = new Counter('v1_preview_success');
export const v2Success = new Counter('v2_preview_success');
export const v1ErrorRate = new Rate('v1_preview_errors');
export const v2ErrorRate = new Rate('v2_preview_errors');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const V1_FILE_ID = __ENV.V1_FILE_ID || '1';
const V2_FILE_ID = __ENV.V2_FILE_ID || '1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export const options = {
    scenarios: {
        // Multi-stage constant arrival rate to test 10, 25, 50, 100, 200 RPS
        preview_v1_load: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 300,
            stages: [
                { target: 10, duration: '20s' },  // 10 RPS
                { target: 25, duration: '20s' },  // 25 RPS
                { target: 50, duration: '20s' },  // 50 RPS
                { target: 100, duration: '20s' }, // 100 RPS
                { target: 200, duration: '20s' }, // 200 RPS
                { target: 0, duration: '10s' },   // ramp down
            ],
            exec: 'testV1Preview',
            tags: { endpoint: 'v1_preview' },
        },
        preview_v2_load: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 300,
            startTime: '120s', // Run V2 after V1 to benchmark in isolated conditions
            stages: [
                { target: 10, duration: '20s' },  // 10 RPS
                { target: 25, duration: '20s' },  // 25 RPS
                { target: 50, duration: '20s' },  // 50 RPS
                { target: 100, duration: '20s' }, // 100 RPS
                { target: 200, duration: '20s' }, // 200 RPS
                { target: 0, duration: '10s' },   // ramp down
            ],
            exec: 'testV2Preview',
            tags: { endpoint: 'v2_preview' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<500'], // SLO P95 < 500ms
        'http_req_failed': ['rate<0.01'],    // SLO Error rate < 1%
        'v1_preview_duration': ['p(95)<500'],
        'v2_preview_duration': ['p(95)<500'],
        'v1_preview_errors': ['rate<0.01'],
        'v2_preview_errors': ['rate<0.01'],
    },
};

const getHeaders = () => {
    const headers = {
        'Accept': 'image/svg+xml, */*',
    };
    if (AUTH_TOKEN) {
        headers['Authorization'] = `Bearer ${AUTH_TOKEN}`;
    }
    return headers;
};

export function testV1Preview() {
    const url = `${BASE_URL}/api/internal/user-files/${V1_FILE_ID}/preview`;
    const res = http.get(url, { headers: getHeaders() });

    const ok = check(res, {
        'v1 status is 200': (r) => r.status === 200,
        'v1 content-type is svg': (r) => (r.headers['Content-Type'] || '').includes('image/svg+xml'),
        'v1 body is not empty': (r) => r.body && r.body.length > 0,
    });

    v1Duration.add(res.timings.duration);
    if (ok) {
        v1Success.add(1);
        v1ErrorRate.add(0);
    } else {
        v1ErrorRate.add(1);
    }
}

export function testV2Preview() {
    const url = `${BASE_URL}/api/internal/v2/user-files/${V2_FILE_ID}/preview`;
    const res = http.get(url, { headers: getHeaders() });

    const ok = check(res, {
        'v2 status is 200': (r) => r.status === 200,
        'v2 content-type is svg': (r) => (r.headers['Content-Type'] || '').includes('image/svg+xml'),
        'v2 body is not empty': (r) => r.body && r.body.length > 0,
    });

    v2Duration.add(res.timings.duration);
    if (ok) {
        v2Success.add(1);
        v2ErrorRate.add(0);
    } else {
        v2ErrorRate.add(1);
    }
}
