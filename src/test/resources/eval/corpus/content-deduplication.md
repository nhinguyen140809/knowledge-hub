# Content Deduplication

Deduplication hashes chunk content so identical text is embedded and stored only once. When a file changes, an unchanged chunk keeps its hash and is not re-embedded, saving cost during incremental updates.
