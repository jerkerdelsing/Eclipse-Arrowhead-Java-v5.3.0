# Arrowhead GSoSDD network update

The current network-deployment update covers:

- GSoSDD_Arrowhead_Core_Systems_5.3.tex (document version 5.3.0).
- GSoSDD_Arrowhead_Support_Systems_5.3.1.tex (document version 5.3.1).

Each source builds its matching PDF with latexmk -pdf -interaction=nonstopmode -halt-on-error <source.tex> from this folder. Both use sections/network-deployment.tex and figures/network-deployment-model.tex; the Core source selects the Core-specific deployment introduction. Network contracts come from the supplied Arrowhead-5.2 Papyrus UML model. Actual address plans, network controls and commissioning evidence remain deployment-specific.

The older support 5.3 filename carries a 5.2.2 document header and is not the current support edition; it and the 5.2/5.2.2 sources/PDFs are retained unchanged. No running cloud or model was modified. See ../NETWORK-MODEL-UPDATE-2026-11-10.md for provenance and validation.
