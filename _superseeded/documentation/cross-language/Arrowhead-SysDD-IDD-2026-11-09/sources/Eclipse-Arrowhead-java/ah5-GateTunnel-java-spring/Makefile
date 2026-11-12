.PHONY: build test verify image deploy stop docs
build:
	mvn -B clean verify
test:
	mvn -B test
verify:
	sh scripts/verify.sh
image:
	docker compose build
deploy:
	docker compose up -d --build
stop:
	docker compose down
docs:
	cd documentation/v5.2.2 && latexmk -pdf -interaction=nonstopmode -halt-on-error *-SysDD.tex
