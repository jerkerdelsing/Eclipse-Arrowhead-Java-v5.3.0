#!/bin/sh
# Compiles every generated document in parallel and reports failures, undefined references and overfull boxes.
# Usage: check-docs.sh <ah5-core-java-spring directory>. Leaves the LaTeX auxiliary files in place (see .gitignore).
set -u
CORE="$1"
for d in "$CORE"/*/documentation/v5.2.2; do
  ( cd "$d" && for f in ./*.tex; do
      b=$(basename "$f" .tex)
      if latexmk -pdf -interaction=nonstopmode -halt-on-error -quiet "$f" >/dev/null 2>&1; then
        u=$(grep -cE 'LaTeX Warning: (Reference|Citation).*undefined' "$b.log")
        o=$(grep -c 'Overfull \\hbox' "$b.log")
        big=$(grep -oE 'Overfull \\hbox \(([0-9]+)' "$b.log" | grep -oE '[0-9]+$' | awk '$1>=10' | wc -l | tr -d ' ')
        echo "OK   $b undefined=$u overfull=$o overfull10pt=$big"
      else
        echo "FAIL $b: $(grep -m1 -A2 '^!' "$b.log" | tr '\n' ' ')"
      fi
    done ) &
done
wait
