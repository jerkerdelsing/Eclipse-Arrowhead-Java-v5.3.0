#!/bin/sh
# Builds the PDFs of this directory (latexmk with pdfLaTeX and BibTeX) and removes the auxiliary files.
set -eu
cd "$(dirname "$0")"
for source in ./*.tex; do
  latexmk -pdf -interaction=nonstopmode -halt-on-error -quiet "$source"
done
latexmk -c -quiet
rm -f ./*.bbl
echo "SysMonitor documentation PDFs are current."
