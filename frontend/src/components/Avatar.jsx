import { useState } from 'react';
import './Avatar.css';

export default function Avatar({ person, size = 'medium' }) {
    const [failedUrl, setFailedUrl] = useState(null);
    const name = person.displayName || person.username || 'Користувач';
    const initials = person.initials || name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
    return (
        <span className={`feed-avatar feed-avatar--${person.tone || 'violet'} feed-avatar--${size}`}>
            {person.avatarUrl && failedUrl !== person.avatarUrl
                ? <img src={person.avatarUrl} alt={`Аватар ${name}`} onError={() => setFailedUrl(person.avatarUrl)} />
                : initials}
        </span>
    );
}
