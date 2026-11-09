# Arrowhead v4.6 and v5.0 - SysML - DSL and library

> **Primary source:** This is the primary Java implementation tree for further updates in this Eclipse workspace, designated on 2026-10-30. See [primary-source policy](../PRIMARY-SOURCES.md). Historical source-copy notes below describe provenance, not current development ownership.

Arrowhead SysML profile and library.

A profile supporting modelling of Eclipse Arrowhead compliant services, systems, devices and local clouds for v4.6.1.
It's complemented by a library of Eclipse Arrowhead core systems v5.0 primary.
All modeling is made in Eclispe Papyrus.

The MagicDraw implmentations has been discontinued and can be regarded as obsolute.

An updated profile based on a detailed UML metamodel of the Eclipse Arrowhead architecture is now developed using Eclipse Papyrus. 

## Local-cloud configuration

[Java LocalCloudConfigurator](ah5-LocalCloudConfigurator-java/README.md) and the Java LocalCloudManager LC view provide catalog-based graphical cloud composition, saved designs and reviewed ColonyOS deployment workflows. New designs default to the core systems minus SimpleOrchestration plus required infrastructure.
