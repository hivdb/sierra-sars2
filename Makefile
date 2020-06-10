VERSION=$(shell date -u +"%Y%m%d%H%M%S")
DOCKERREPO=$(shell ./scripts/get-docker-repo.sh)

build/libs/Sierra-SARS2.war: $(shell find . -type f -not -path "./.git*" -a -not -path "*.class" -a -not -path "*/.DS_Store" -a -not -path "*/.gradle/*" -a -not -path "*/build/*" -a -not -path "*/target/*" -a -not -path "*/.settings/*" -a -not -path "*.log" -a -not -path "*/__output/*" -a -not -path "docker/*" | sed 's#\([| ]\)#\\\1#g')
	@mkdir -p .gradle
	@docker run -it --rm -v $(shell realpath ${PWD}):/sierra -v $(shell realpath ${PWD})/.gradle:/root/.gradle -w /sierra --entrypoint /sierra/gradlew hivdb/tomcat-with-nucamino:latest assemble
	@mv build/libs/Sierra-SARS2-*.war build/libs/Sierra-SARS2.war 2>/dev/null || true

build: build/libs/Sierra-SARS2.war
	@docker build -t ${DOCKERREPO} .

force-build: build/libs/Sierra-SARS2.war
	@docker build --no-cache -t ${DOCKERREPO} .

dev: build
	@docker rm -f hivdb-sierra-sars2-dev 2>/dev/null || true
	@docker run \
		--name=hivdb-sierra-sars2-dev \
		--volume ~/.aws:/root/.aws:ro \
		--env NUCAMINO_AWS_LAMBDA=nucamino:3 \
		--rm -it --publish=8113:8080 ${DOCKERREPO} dev

release: build
	@docker login
	@docker tag ${DOCKERREPO}:latest ${DOCKERREPO}:${VERSION}
	@docker push ${DOCKERREPO}:${VERSION}
	@docker push ${DOCKERREPO}:latest
	@echo ${VERSION} > .latest-version
	@sleep 2
