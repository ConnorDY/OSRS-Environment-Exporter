@echo off
java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED -jar ./build/libs/osrs-environment-exporter-fat-2.1.0.jar
