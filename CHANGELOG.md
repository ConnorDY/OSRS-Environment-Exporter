# Changelog

## [Unreleased]

### :sparkles: Enhancements

- Add giants' foundry to location search &ndash; [#88](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/88) @ConnorDY
  - Also removed duplicate locations and added numbers for locations that span multiple regions.
  - Special thanks to Uber (@LengaJenga) for reporting that this location was missing.

- Add MacOS support &ndash; [#86](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/86) @ScoreUnder
  - The renderer is now using LWJGL, which also fixes a couple of weird input/focus issues we were having.

- Add mouse warping and antialiasing settings &ndash; [#86](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/86) @ScoreUnder

- Add option to choose render sorter, which may fix buggy previews on some machines &ndash; [#86](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/86) @ScoreUnder
  - Also fix shaders exceeding their workgroup size, which caused that issue in the first place.

- Provide instant feedback when changing settings &ndash; [#93](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/93) @ScoreUnder

- Include alpha values in exported non-textured faces &ndash; [#99](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/99) @ScoreUnder
  - Fix glTF linter issues along the way.

- Add alpha blending mode selection &ndash; [#100](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/100) @ScoreUnder

### :bug: Bug Fixes

- Fix rotation of certain objects &ndash; [#98](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/98) @ConnorDY

- Made input smoother when FPS limit is turned on &ndash; [#97](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/97) @ScoreUnder

- Multiple cache decoding fixes &ndash; [#106](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/106) @ScoreUnder
  - Fix position and rotation of wall-attached objects (shields, coats of arms, windows, torches, etc.).
  - Add normal-merged lighting algorithm, removing the pillow-shaded effect some tiles erroneously had.

### :wrench: Maintenance

- Optimise glTF export a little &ndash; [#96](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/96) @ScoreUnder

## 2.1.0

### :sparkles: Enhancements

- Provide the option to input a grid of region IDs to load &ndash; [#79](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/74) @ConnorDY & @ScoreUnder
  - Allows more fine-grained control of the regions exported, as well as loading non-square areas.

- Check if a newer version of the OSRS Environment Exporter is available after the cache chooser screen &ndash; [#84](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/84) @ConnorDY
  - This check can be disabled from the "Preferences" menu.

### :bug: Bug Fixes

- Fix missing lava textures in the Volcanic Mine &ndash; [#83](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/83) @ScoreUnder
  - Also fix missing torches in Falador and Lumbridge
  - Secret debug menu to help find these problems in the first place??

## 2.0.2

### :sparkles: Enhancements

- Allow setting the initial region ID + radius in config &ndash; [#74](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/74) @ConnorDY

- Automatically focus on the first input when opening windows &ndash; [#68](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/68) @ConnorDY

### :bug: Bug Fixes

- Fix colour blending in the wilderness &ndash; [#75](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/75) @ScoreUnder

### :wrench: Maintenance

- Refactor some colour/brightness-related code &ndash; [#73](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/73) @ScoreUnder

- Make `RegionDefinition` tile array elements not nullable &ndash; [#71](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/71) @ScoreUnder

- Make `CalcTileColor` a little less mysterious &ndash; [#70](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/70) @ScoreUnder

- Clean up `ColorPalette` code by merging RuneLite code into it &ndash; [#67](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/67) @ScoreUnder

- Combine small and large comp shaders &ndash; [#65](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/65) @ConnorDY

## 2.0.1

### :sparkles: Enhancements

- Update locations data with a more complete data set &ndash; [#61](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/61) @ConnorDY
  - Special thanks to Uber (@LengaJenga) for finding this additional data set!

### :wrench: Maintenance

- Dead code removal & cleanup around `computeObjs` &ndash; [#55](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/55) @ScoreUnder

- Use the correct `UIManager` key in `NumericTextField` &ndash; [#60](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/60) @ScoreUnder

## 2.0.0

### :sparkles: Enhancements

- Export using glTF format instead of OBJ/PLY &ndash; [#28](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/28) @ConnorDY
  - Allowed us to enable alpha textures and remove default specularity
  - Removed the need for a Python script that was previously used to convert OBJ to PLY
  - Greatly reduced load times when importing in Blender compared to the old OBJ/PLY format

- Add new "Location Search" feature &ndash; [#41](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/41) @ConnorDY
  - Allows users to select a location from a searchable list

- Create a unique, timestamped output directory for each export &ndash; [#10](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/10) @ConnorDY
  - Ensures new exports do not overwrite old ones

- Reduced load times **significantly** by implementing negative region caching &ndash; [d2fd490](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/d2fd490a79044a9d9df28d816308e08716734cb3) @ScoreUnder
  - Load times that would previously take almost a minute are now instantaneous (*on our machines*)

- Add a setting for limiting the frame rate &ndash; [#48](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/48) @ScoreUnder & @ConnorDY
  - This can be used to reduce power consumption

### :bug: Bug Fixes

- Scale models and offset height values when requested &ndash; [#15](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/15) @ScoreUnder
  - This fixed weird height/floating issues seen in areas like Falador or the Slayer Tower

- Fix buffer underflow when loading certain regions &ndash; [#44](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/44) @ScoreUnder
  - This also made it possible to skip loading regions that would previously cause the tool to crash

- Load region tile data even when region xtea keys are missing &ndash; [#49](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/49) @ScoreUnder

- Ensure process exits when all windows are closed &ndash; [870d24a](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/870d24a8169b74ed446c32701fd4da3dc3fd77aa) @ScoreUnder

- Include Z-level in tile colour calculation &ndash; [752c66c](https://github.com/ConnorDY/OSRS-Environment-Exporter/commit/752c66c70f0ce6e7d2a2df9210e4a6d395740558) @ScoreUnder

### :wrench: Maintenance

- Replaced JavaFX with Swing &ndash; [#52](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/52) @ScoreUnder

- Added a [GitHub Workflow](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) for automating and archiving builds &ndash; [#12](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/12) @ScoreUnder

- Added the automated linting tool, [ktlint](https://ktlint.github.io/) &ndash; [#32](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/32) @ConnorDY

- Replaced `println()` calls with [Logback](https://github.com/qos-ch/logback) for logging &ndash; [#19](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/19) @ConnorDY

- Update Gradle to version `7.3.2` &ndash; [5f5558](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/5f5558d2783624a96148d389b4ee72500033f795) @ScoreUnder
