import {defineConfig} from '@playwright/test';
import process from 'node:process';
export default defineConfig({
    testDir:'./e2e',testMatch:'**/*.spec.mjs',fullyParallel:false,workers:1,
    globalSetup:'./e2e/reset-test-state.mjs',
    timeout:90000,expect:{timeout:15000},forbidOnly:!!process.env.CI,retries:0,
    reporter:[['list'],['html',{open:'never'}]],
    use:{baseURL:'http://localhost:15173',browserName:'chromium',
        ...(process.platform==='win32'?{channel:'msedge'}:{}),
        trace:'retain-on-failure',screenshot:'only-on-failure',viewport:{width:1440,height:1000}},
    webServer:[
        {command:'node e2e/oidc-server.mjs',url:'http://127.0.0.1:19090',reuseExistingServer:false},
        {command:'node e2e/start-backend.mjs',url:'http://localhost:18080/api/users/me',timeout:120000,reuseExistingServer:false,stdout:'pipe'},
        {command:'node node_modules/vite/bin/vite.js --host localhost --port 15173 --strictPort',
            url:'http://localhost:15173',reuseExistingServer:false,
            env:{E2E_API_TARGET:'http://localhost:18080',VITE_OAUTH_BASE_URL:'http://localhost:18080'}}
    ]
});
