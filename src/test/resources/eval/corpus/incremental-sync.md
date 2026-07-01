# Incremental Sync

Incremental sync brings an index up to date by processing only changed files since the last run. It re-indexes modifications, evicts chunks a file no longer has, and skips unchanged content, keeping queries fast meanwhile.
