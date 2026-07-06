# Convenience wrapper so a full run is one or two commands. All targets are run from the
# perf/ directory: `make up`, `make seed`, `make bench` (or `make all`).
#
# The app + Neo4j + Qdrant stack is managed from the repo root via docker compose with
# the perf override, so `up`/`down` cd there.

REPO_ROOT := $(shell git rev-parse --show-toplevel)
COMPOSE := docker compose -f docker-compose.yml -f perf/compose.perf.yml
PY := python3

.PHONY: help venv up down logs seed bench all doctor clean

help:
	@echo "make venv    # create .venv and install deps"
	@echo "make up      # build image + start app/neo4j/qdrant with the perf data mount"
	@echo "make seed    # register sources, index, create principals (run once after up)"
	@echo "make bench   # run all enabled scenarios and write results/"
	@echo "make all     # seed then bench"
	@echo "make doctor  # check the stack is reachable"
	@echo "make down    # stop the stack"

venv:
	$(PY) -m venv .venv
	./.venv/bin/pip install -q -r requirements.txt
	@echo "Activate with: source perf/.venv/bin/activate"

up:
	cd $(REPO_ROOT) && $(COMPOSE) up -d --build

down:
	cd $(REPO_ROOT) && $(COMPOSE) down

logs:
	cd $(REPO_ROOT) && $(COMPOSE) logs -f app

seed:
	$(PY) run.py seed

bench:
	$(PY) run.py bench

all:
	$(PY) run.py all

doctor:
	$(PY) run.py doctor

clean:
	rm -rf results/*.json results/*.md __pycache__ harness/__pycache__ harness/scenarios/__pycache__
