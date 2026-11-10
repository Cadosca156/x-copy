import { createPost } from './postApi.js';

// Retain the key after an uncertain response. Editing the payload starts a new submission.
export function createPostSubmission(send = createPost, newKey = () => crypto.randomUUID()) {
    let pending = null;
    let inFlight = null;
    return (content, file) => {
        if (inFlight) return inFlight;
        if (!pending || pending.content !== content || pending.file !== file) {
            pending = { content, file, key: newKey() };
        }
        inFlight = Promise.resolve().then(() => send(content, file, pending.key)).then(post => {
            pending = null;
            return post;
        }).finally(() => { inFlight = null; });
        return inFlight;
    };
}
