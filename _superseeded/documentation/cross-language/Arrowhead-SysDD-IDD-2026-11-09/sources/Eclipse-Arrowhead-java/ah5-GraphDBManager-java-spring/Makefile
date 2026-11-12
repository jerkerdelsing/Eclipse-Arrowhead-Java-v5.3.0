.PHONY: build test image deploy stop
build:
	mvn -B clean verify
test:
	mvn -B test
image:
	docker compose build
deploy:
	docker compose up -d --build
stop:
	docker compose down
