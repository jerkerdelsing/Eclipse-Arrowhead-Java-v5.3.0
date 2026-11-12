.PHONY: build test image deploy stop docs
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
docs:
	cd documentation/v5.2.2 && latexmk -pdf -interaction=nonstopmode -halt-on-error *-SysDD.tex
