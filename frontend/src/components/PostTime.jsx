const localDateTime = new Intl.DateTimeFormat('uk-UA', {
    dateStyle: 'short',
    timeStyle: 'short',
});
const hasOffset = /(?:Z|[+-]\d{2}:\d{2})$/i;

export default function PostTime({ createdAt, className }) {
    if (!createdAt) return null;

    // The Docker backend sends UTC LocalDateTime values without an offset.
    const date = new Date(hasOffset.test(createdAt) ? createdAt : `${createdAt}Z`);
    if (!Number.isFinite(date.getTime())) return null;

    return <time className={className} dateTime={date.toISOString()}>{localDateTime.format(date)}</time>;
}
