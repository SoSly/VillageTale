# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/SoSly/VillageTale/tree/1.20.1)

### Added
**Villagers**
- New custom Villager entity
- Villager profession system with many professions available:
    - Commoner (unemployed)
    - Farmer (tills soil, plants seeds, harvests crops)

**Villages & Zones**
- Town Hall block - establish your village (automatically named after you)
- Villages must be at least 30 chunks apart
- Zone system for defining areas within villages
- Four zone shapes available: rectangle, sphere, point, or route
- Many zone types available:
    farmland, home, storage, townhall
- Zones can be filtered to only accept specific items (e.g., wheat-only farms)

**Commands**
- `/vt assign` - Assign homes to villagers
- `/vt hunger` - Check villager hunger status
- `/vt exhaust` - Add exhaustion to villagers (for testing)
- `/vt village create/remove/list/info` - Manage villages
- `/vt villager <targets> profession [<profession>]` - View or set villager professions
- `/vt zone create/delete/rename/info/list` - Manage village zones
- `/vt zone path add/clear` - Build custom path-shaped zones
- `/vt zone claim/release` - Temporarily claim positions within zones for villagers
- `/vt zone filter add/remove/clear/list` - Control what items zones accept (e.g., limit farmland to specific crops)
