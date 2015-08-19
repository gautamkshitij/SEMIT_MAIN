#!/bin/bash
mkdir temp
mkdir temp/javasrc
mkdir temp/javasrc/src
mkdir temp/javasrc/src/drjava
mkdir temp/javasrc/src/drjava/smyle
mkdir temp/javasrc/src/drjava/smyle/meta
perl bin/gj2java.pl src/drjava/smyle/*.java src/drjava/smyle/meta/*.java
