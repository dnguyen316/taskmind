.PHONY: build test vibe-verify vibe-token-record vibe-token-report
build:
	mvn clean install -DskipTests
test:
	mvn test
vibe-verify:
	./scripts/vibe-verify.sh

vibe-token-record:
	python3 scripts/vibe-token-usage.py record $(filter-out $@,$(MAKECMDGOALS))

vibe-token-report:
	python3 scripts/vibe-token-usage.py report $(filter-out $@,$(MAKECMDGOALS))

%:
	@:
