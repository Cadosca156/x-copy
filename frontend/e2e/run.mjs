import {spawn} from 'node:child_process';
import path from 'node:path';
import process from 'node:process';
const env = {...process.env};
if(process.platform==='win32') {
    // Playwright uses taskkill to stop its server process trees on Windows.
    const pathKey = Object.keys(env).find(key=>key.toLowerCase()==='path') || 'Path';
    env[pathKey] = path.join(env.SystemRoot || 'C:/Windows','System32')+';'+path.dirname(process.execPath)+';'+(env[pathKey] || '');
}
const child = spawn(process.execPath,['node_modules/@playwright/test/cli.js','test',...process.argv.slice(2)],{stdio:'inherit',env});
child.on('error',error=>{console.error(error);process.exitCode=1;});
child.on('exit',code=>process.exit(code??1));
