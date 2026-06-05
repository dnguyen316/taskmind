.PHONY: build test vibe-verify vibe-token-record vibe-token-report
build:
	cd apps/backend && mvn clean install -DskipTests
test:
	cd apps/backend && mvn test
vibe-verify:
	./scripts/vibe-verify.sh


vibe-token-record:
	python3 scripts/vibe-token-usage.py record $(filter-out $@,$(MAKECMDGOALS))

vibe-token-report:
	python3 scripts/vibe-token-usage.py report $(filter-out $@,$(MAKECMDGOALS))

%:
	@:
