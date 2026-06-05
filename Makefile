.PHONY: build test vibe-verify
build:
	cd apps/backend && mvn clean install -DskipTests
test:
	cd apps/backend && mvn test
vibe-verify:
	./scripts/vibe-verify.sh
