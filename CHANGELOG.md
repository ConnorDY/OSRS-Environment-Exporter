# Changelog

## [Unreleased]

### :sparkles: Enhancements

- Export using glTF format instead of OBJ/PLY – [#28](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/28) @ConnorDY
  - Allowed us to enable alpha textures and remove default specularity
  - Removed the need for a Python script that was previously used to convert OBJ to PLY
  - Greatly reduced load times when importing in Blender compared to the old OBJ/PLY format

- Create a unique, timestamped output directory for each export – [#10](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/10) @ConnorDY
  - Ensures new exports do not overwrite old ones

- Reduced load times **significantly** by implementing negative region caching – [d2fd490](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/d2fd490a79044a9d9df28d816308e08716734cb3) @ScoreUnder
  - Load times that would previously take almost a minute are now instantaneous (*on our machines*)

### :bug: Bug Fixes

- Scale models and offset height values when requested – [#15](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/15) @ScoreUnder
    - This fixed weird height/floating issues seen in areas like Falador or the Slayer Tower

- Ensure process exits when all windows are closed – [870d24a](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/870d24a8169b74ed446c32701fd4da3dc3fd77aa) @ScoreUnder

- Include Z-level in tile colour calculation – [752c66c](https://github.com/ConnorDY/OSRS-Environment-Exporter/commit/752c66c70f0ce6e7d2a2df9210e4a6d395740558) @ScoreUnder

### :wrench: Maintenance

- Added a [GitHub Workflow](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) for automating and archiving builds – [#12](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) @ScoreUnder

- Added automated linting tool: [ktlint](https://ktlint.github.io/) – [#32](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/32) @ConnorDY

- Replaced `println()` calls with [Logback](https://github.com/qos-ch/logback) for logging – [#19](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/19) @ConnorDY

- Update Gradle to version `7.3.2` – [5f5558](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/5f5558d2783624a96148d389b4ee72500033f795) @ScoreUnder
