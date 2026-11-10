// Preserve delivery spelling. Only the server derives the case-insensitive account key.
export function emailInput(value) {
    return typeof value === 'string' ? value.trim() : value;
}
