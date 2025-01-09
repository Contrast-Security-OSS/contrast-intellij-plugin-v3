VERSION_BUMP_BRANCH ?= 'version-bump'
BUILD_VERSION_ARGS ?= -Psnapshot=true
GRADLEW ?= ./gradlew
GRADLEW_ARGS = $(BUILD_VERSION_ARGS) --stacktrace
DOCKER_TAG ?= contrast-intellij-plugin
DOCKER_VERSION ?= dev

.PHONY: clean
clean:
	$(GRADLEW) clean $(GRADLEW_ARGS)

.PHONY: test
test:
	$(GRADLEW) test $(GRADLEW_ARGS)


.PHONY: build
build:
	$(GRADLEW) clean build $(GRADLEW_ARGS)

.PHONY: buildPlugin
buildPlugin:
	$(GRADLEW) clean buildPlugin $(GRADLEW_ARGS)

.PHONY: run
run:
	$(GRADLEW) runIde $(GRADLEW_ARGS)

.PHONY: toolchain
toolchain:
	docker build -f Dockerfile.toolchain -t local/contrast-intellij-plugin .


.PHONY: docker-image
docker-image:
	GOOS=linux $(MAKE) $(OTELCOL)
	docker build -t $(DOCKER_TAG):$(DOCKER_VERSION) .
