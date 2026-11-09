.PHONY: build test image deploy stop
build: image
test:
	sh scripts/verify.sh
image:
	docker compose build
deploy:
	docker compose up -d --build
stop:
	docker compose down
