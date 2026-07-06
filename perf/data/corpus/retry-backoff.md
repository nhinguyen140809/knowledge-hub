# Retry With Exponential Backoff

Retrying a failed call with exponential backoff waits progressively longer between attempts, reducing pressure on a struggling dependency. A capped number of attempts prevents infinite loops before the failure is finally surfaced.
