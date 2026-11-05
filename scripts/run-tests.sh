#!/usr/bin/env bash
# Runs the backend unit tests in Docker (same Maven image as the Dockerfile),
# on a temporary copy so the repo never gets root-owned files.
cd "$(dirname "$0")/.."

D=$(mktemp -d /tmp/nexb-test.XXXXXX)
tar --exclude=target -C backend -cf - . | tar -C "$D" -xf -

docker run --rm -v "$D":/app:z -v nexb-m2:/root/.m2 -w /app \
  maven:3.9.6-eclipse-temurin-17 mvn -B test > /tmp/mvn-test.log 2>&1
STATUS=$?

# target/ was created by root inside the container, so remove it the same way
docker run --rm -v "$D":/app:z alpine sh -c 'rm -rf /app/* /app/.[!.]*' >/dev/null 2>&1
rmdir "$D" 2>/dev/null

echo "exit code: $STATUS"
grep -E "Tests run:|BUILD|ERROR" /tmp/mvn-test.log | tail -8
exit $STATUS
