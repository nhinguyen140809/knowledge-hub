# Knowledge Hub — dev tasks. Run `just` to list, `just <task>` to run.
set dotenv-load := true
set shell := ["bash", "-cu"]

mvn := "./mvnw"

# list available tasks
default:
    @just --list

# run the app locally (dev profile)
dev:
    {{mvn}} spring-boot:run

# compile sources (regenerates @ConfigurationProperties metadata)
compile:
    {{mvn}} -B compile

# compile & package (skip tests)
build:
    {{mvn}} -B -DskipTests package

# run tests (needs Docker for Testcontainers). SPRING_PROFILES_ACTIVE is cleared because
# dotenv-load pulls in .env's SPRING_PROFILES_ACTIVE=prod for the app container, and the prod
# profile requires a real SPRING_NEO4J_URI — breaking Testcontainers' own dynamic URI override.
test:
    SPRING_PROFILES_ACTIVE= {{mvn}} -B test

# same suite, explicit name: embedding is mocked deterministically, no network calls, safe for CI
test-mock: test

# real-provider retrieval eval: live embedding calls against EMBEDDING_API_KEY in .env — needs a
# working (non-rate-limited) provider and a model whose dimension matches app.embedding.dimension
test-real:
    SPRING_PROFILES_ACTIVE= EVAL_ASSERT_THRESHOLDS=true {{mvn}} -B test -Dtest=RetrievalEvalThresholdTests

# live Git-connector indexing against a real public repo — needs outbound network to GitHub
test-git-live:
    SPRING_PROFILES_ACTIVE= GIT_LIVE_TEST=true {{mvn}} -B test -Dtest=GitSourceLiveIndexingTests

# format check + build + tests
verify:
    {{mvn}} -B spotless:check verify

# auto-format code (Spotless)
format:
    {{mvn}} -B spotless:apply

# lint (Spotless check + Checkstyle)
lint:
    {{mvn}} -B spotless:check checkstyle:check

# remove build output
clean:
    {{mvn}} -B clean

# print the dependency tree
deps:
    {{mvn}} -B dependency:tree

# build the jar, then run it
run-jar: build
    java -jar target/*.jar

# start the full stack (app + Neo4j) in containers
up:
    docker compose up -d --build

# stop the stack
down:
    docker compose down

# stop the stack and delete volumes (wipes Neo4j + Qdrant data)
down-v:
    docker compose down -v

# restart the stack
restart: down up

# pause the running containers without removing them — frees the RAM/CPU they were using
# (handy for a Codespace left idle for hours) while keeping all data and network state, so
# `just resume` brings the exact same stack back with no rebuild and no re-seed
pause:
    docker compose stop

# resume a stack paused with `just pause` — much faster than `up`, nothing to rebuild
resume:
    docker compose start

# tail application logs
logs:
    docker compose logs -f app

# show running containers
ps:
    docker compose ps

# enable the git pre-commit hook (auto-format on commit)
hooks:
    git config core.hooksPath .githooks && echo "git hooks enabled"
