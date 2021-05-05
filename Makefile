VERSION=$(shell date -u +"%Y%m%d%H%M%S")
DOCKERREPO=$(shell ./scripts/get-docker-repo.sh)

sync-drdb:
	@cp -v $$(ls ../chiro-cms/downloads/covid-drdb/*.db | sort -r | head -1) src/main/resources/covid-drdb.db

build:
	@docker build -t ${DOCKERREPO} .

force-build:
	@docker build --no-cache -t ${DOCKERREPO} .

inspect-dev:
	@docker exec -it hivdb-sierra-sars2-dev /bin/bash

dev: build
	@docker rm -f hivdb-sierra-sars2-dev 2>/dev/null || true
	@docker run \
		--name=hivdb-sierra-sars2-dev \
		--volume ~/.aws:/root/.aws:ro \
		--env NUCAMINO_AWS_LAMBDA=nucamino-align-with:5 \
		--rm -it --publish=8113:8080 ${DOCKERREPO} dev

release: build
	@docker login
	@docker tag ${DOCKERREPO}:latest ${DOCKERREPO}:${VERSION}
	@docker push ${DOCKERREPO}:${VERSION}
	@docker push ${DOCKERREPO}:latest
	@echo ${VERSION} > .latest-version
	@sleep 2

.PHONY: build
