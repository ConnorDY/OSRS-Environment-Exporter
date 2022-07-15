# Changelog

## [Unreleased]

### :sparkles: Enhancements

- Use a unique, timestamped output directory for each export ([#10](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/10) - @ConnorDY)
- Use negative region caching for much faster region loading ([d2fd490](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/d2fd490a79044a9d9df28d816308e08716734cb3) - @ScoreUnder)

### :bug: Bug Fixes

- Remove default specularity from all materials in `col.mtl` ([8750dd1](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/8750dd12a8a7a897e4ae054ee76c0a5ab81ef158) - @ConnorDY)
- Ensure process exits when all windows are gone ([870d24a](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/870d24a8169b74ed446c32701fd4da3dc3fd77aa) - @ScoreUnder)
- Include Z-level in tile colour calculation ([752c66c](https://github.com/ConnorDY/OSRS-Environment-Exporter/commit/752c66c70f0ce6e7d2a2df9210e4a6d395740558) - @ScoreUnder)
- Scale models and offset height values when requested ([9602775](https://github.com/ConnorDY/OSRS-Environment-Exporter/commit/96027751852b3d4ac52962e83c444e2b069ff277) - @ScoreUnder)

### :wrench: Maintenance

- Update Gradle to version `7.3.2` ([5f5558](https://github.com/ConnorDY/OSRS-Environment-Exporter/pull/6/commits/5f5558d2783624a96148d389b4ee72500033f795) - @ScoreUnder)
