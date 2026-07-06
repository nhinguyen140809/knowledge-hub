# Cache Eviction And TTL

A retrieval cache stores recent query results keyed by the query and the caller's readable sources. A time-to-live bounds staleness and a maximum size evicts the least recently used entry when the cache is full.
