-- Token bucket, evaluated atomically inside Redis.
-- KEYS[1] = bucket key
-- ARGV[1] = capacity (max tokens, the tier limit)
-- ARGV[2] = refill window in ms (time to refill an empty bucket)
-- ARGV[3] = current time in ms
-- Returns {allowed (1/0), whole tokens left, ms until next token, ms until full}
--
-- The bucket level is stored in units of 1/window of a token, so refill
-- (elapsed ms * capacity) stays in exact integer arithmetic.

local capacity = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local full = capacity * window

local bucket = redis.call('HMGET', KEYS[1], 'level', 'ts')
local level = tonumber(bucket[1])
local ts = tonumber(bucket[2])

if level == nil or ts == nil then
    level = full
    ts = now
end

local elapsed = math.max(0, now - ts)
level = math.min(full, level + elapsed * capacity)

local allowed = 0
if level >= window then
    level = level - window
    allowed = 1
end

redis.call('HSET', KEYS[1], 'level', string.format('%.0f', level), 'ts', string.format('%.0f', now))
redis.call('PEXPIRE', KEYS[1], window)

local retryAfter = 0
if level < window then
    retryAfter = math.ceil((window - level) / capacity)
end

local resetAfter = math.ceil((full - level) / capacity)

return {allowed, math.floor(level / window), retryAfter, resetAfter}
