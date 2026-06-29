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

# run tests (needs Docker for Testcontainers)
test:
    {{mvn}} -B test

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

# restart the stack
restart: down up

# tail application logs
logs:
    docker compose logs -f app

# show running containers
ps:
    docker compose ps

# enable the git pre-commit hook (auto-format on commit)
hooks:
    git config core.hooksPath .githooks && echo "git hooks enabled"
