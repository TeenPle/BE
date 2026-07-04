insert into content_filter_term
    (language, term, normalized_term, severity, category, source, source_key, enabled, created_at, updated_at)
values
    ('ko', _utf8mb4 0xEC8B9CEBB09C, _utf8mb4 0xEC8B9CEBB09C, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEC94A8EBB09C, _utf8mb4 0xEC94A8EBB09C, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xE38585E38582, _utf8mb4 0xE38585E38582, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xE38586E38582, _utf8mb4 0xE38586E38582, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEAB09CEC8388EB81BC, _utf8mb4 0xEAB09CEC8388EB81BC, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEC8388EB81BC, _utf8mb4 0xEC8388EB81BC, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEBB391EC8BA0, _utf8mb4 0xEBB391EC8BA0, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xE38582E38585, _utf8mb4 0xE38582E38585, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEBAFB8ECB99CEB8688, _utf8mb4 0xEBAFB8ECB99CEB8688, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEBAFB8ECB99CEB8584, _utf8mb4 0xEBAFB8ECB99CEB8584, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xECA286, _utf8mb4 0xECA286, 'BLOCK', 'SEXUAL', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xECA1B4EB8298, _utf8mb4 0xECA1B4EB8298, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xECA286EAB099, _utf8mb4 0xECA286EAB099, 'BLOCK', 'PROFANITY', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEABABCECA0B8, _utf8mb4 0xEABABCECA0B8, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp),
    ('ko', _utf8mb4 0xEB8BA5ECB390, _utf8mb4 0xEB8BA5ECB390, 'BLOCK', 'HARASSMENT', 'SYSTEM', 'ko-required-profanity', true, current_timestamp, current_timestamp)
on duplicate key update
    term = values(term),
    normalized_term = values(normalized_term),
    severity = values(severity),
    category = values(category),
    source = values(source),
    source_key = values(source_key),
    enabled = true,
    updated_at = current_timestamp;
