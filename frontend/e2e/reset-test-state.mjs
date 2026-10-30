import {execFileSync} from 'node:child_process';
import process from 'node:process';
export default function resetTestState() {
    const docker = process.platform==='win32'?'C:/Program Files/Docker/Docker/resources/bin/docker.exe':'docker';
    // This exact Compose project owns only the disposable E2E containers.
    execFileSync(docker,['compose','-p','xcopy-e2e','-f','e2e/compose.yaml','exec','-T','redis','redis-cli','FLUSHDB'],{stdio:'pipe'});
}
