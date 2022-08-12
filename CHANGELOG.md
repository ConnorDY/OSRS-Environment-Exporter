# Changelog

## 2.1.0

### :sparkles: Enhancements

- Provide the option to input a grid of region IDs to load &mdash; [#79](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/74) @ConnorDY & @ScoreUnder
  - Allows more fine-grained control of the regions exported, as well as loading non-square areas.

## 2.0.2

### :sparkles: Enhancements

- Allow setting the initial region ID + radius in config – [#74](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/74) @ConnorDY
- Automatically focus on the first input when opening windows – [#68](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/68) @ConnorDY

### :bug: Bug Fixes

- Fix colour blending in the wilderness – [#75](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/75) @ScoreUnder

### :wrench: Maintenance

- Refactor some colour/brightness-related code – [#73](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/73) @ScoreUnder
- Make `RegionDefinition` tile array elements not nullable – [#71](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/71) @ScoreUnder
- Make `CalcTileColor` a little less mysterious – [#70](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/70) @ScoreUnder
- Clean up `ColorPalette` code by merging RuneLite code into it – [#67](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/67) @ScoreUnder
- Combine small and large comp shaders – [#65](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/65) @ConnorDY

## 2.0.1

### :sparkles: Enhancements

- Update locations data with a more complete data set – [#61](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/61) @ConnorDY
  - Special thanks to Uber (@LengaJenga) for finding this additional data set!

### :wrench: Maintenance

- Dead code removal & cleanup around `computeObjs` – [#55](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/55) @ScoreUnder
- Use the correct `UIManager` key in `NumericTextField` – [#60](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/60) @ScoreUnder

## 2.0.0

### :sparkles: Enhancements

- Export using glTF format instead of OBJ/PLY – [#28](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/28) @ConnorDY
  - Allowed us to enable alpha textures and remove default specularity
  - Removed the need for a Python script that was previously used to convert OBJ to PLY
  - Greatly reduced load times when importing in Blender compared to the old OBJ/PLY format

- Add new "Location Search" feature – [#41](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/41) @ConnorDY
  - Allows users to select a location from a searchable list

- Create a unique, timestamped output directory for each export – [#10](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/10) @ConnorDY
  - Ensures new exports do not overwrite old ones

- Reduced load times **significantly** by implementing negative region caching – [d2fd490](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/d2fd490a79044a9d9df28d816308e08716734cb3) @ScoreUnder
  - Load times that would previously take almost a minute are now instantaneous (*on our machines*)

- Add a setting for limiting the frame rate – [#48](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/48) @ScoreUnder & @ConnorDY
  - This can be used to reduce power consumption

### :bug: Bug Fixes

- Scale models and offset height values when requested – [#15](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/15) @ScoreUnder
  - This fixed weird height/floating issues seen in areas like Falador or the Slayer Tower

- Fix buffer underflow when loading certain regions – [#44](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/44) @ScoreUnder
  - This also made it possible to skip loading regions that would previously cause the tool to crash

- Load region tile data even when region xtea keys are missing – [#49](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/49) @ScoreUnder

- Ensure process exits when all windows are closed – [870d24a](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/870d24a8169b74ed446c32701fd4da3dc3fd77aa) @ScoreUnder

- Include Z-level in tile colour calculation – [752c66c](https://github.com/ConnorDY/OSRS-Environment-Exporter/commit/752c66c70f0ce6e7d2a2df9210e4a6d395740558) @ScoreUnder

### :wrench: Maintenance

- Replaced JavaFX with Swing – [#52](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/52) @ScoreUnder

- Added a [GitHub Workflow](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) for automating and archiving builds – [#12](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) @ScoreUnder

- Added the automated linting tool, [ktlint](https://ktlint.github.io/) – [#32](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/32) @ConnorDY

- Replaced `println()` calls with [Logback](https://github.com/qos-ch/logback) for logging – [#19](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/19) @ConnorDY

- Update Gradle to version `7.3.2` – [5f5558](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/5f5558d2783624a96148d389b4ee72500033f795) @ScoreUnder
