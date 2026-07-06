"""Data-driven performance/evaluation harness for Knowledge Hub.

Orchestrates the running app (over its REST API) to measure the acceptance criteria
in docs/report/7-evaluation.tex. Thresholds, workloads, sources and principals are all
declared in perf/config and perf/data; this package only reads them and runs.
"""