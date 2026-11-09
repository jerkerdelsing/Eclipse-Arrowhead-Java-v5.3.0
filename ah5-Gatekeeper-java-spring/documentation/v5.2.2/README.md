# Gatekeeper documentation 5.2.2

The SysDD describes the standalone Java project in this folder as of 25 October 2026. SysD and AMQP publisher implementation references have been updated accordingly. The service contracts remain compatible.

Rebuild with `latexmk -pdf -interaction=nonstopmode -halt-on-error *.tex` from this directory, or `make docs` from the project root for the SysDD. All class, bibliography, logo and template assets are local.

The original generated documentation predates this separation. Maintain these current LaTeX sources directly; running the original combined-tree generator would overwrite the independent packaging changes.
