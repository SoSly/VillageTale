# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/SoSly/VillageTale)

### Added
- Villagers now have three core stats (Physique, Intellect, Endurance) visible in the villager management screen

### Changed
- Recipe knowledge storage format updated (legacy saves will migrate automatically)

### Fixed
- Profession and ZoneType registration now properly fires on the ModBus.

## [Release 0.1.0-alpha](https://github.com/SoSly/VillageTale/releases/tag/0.1.0-alpha)

### Added
**Items and Blocks**
- Ledger: Your town management interface; a book that tracks your villagers and the zones in your village
- Town Hall: The center of your town, and how you get started. Place a town hall to stake your claim in the world
- Added a recipe for the minecraft Bell.

**Management**
- Right-click a vanilla villager with the Ledger to hire them to your village
- Right-click your Town Hall with the Ledger after hiring to assign villagers to your village
- Use the ledger to create zones and manage villagers
- Press F3+V to see village and zone boundaries as an overlay

**Villagers**
- New custom Villager entity
- Villager profession system with many professions available:
    - Commoner (unemployed)
    - Butcher (processes meat and animal products)
    - Carpenter (crafts wooden items and tools)
    - Cook (creates foods from recipes)
    - Farmer (tills soil, plants seeds, harvests crops)
    - Fisher (catches fish and aquatic resources)
    - Forester (cuts trees, plants trees)
    - Herder (manages animals, brings them to pens)
    - Tanner (processes leather and hides)

**Villages & Zones**
- Villages must be at least 30 chunks apart
- Zone system for defining areas within villages
- Four zone shapes available: box, cylinder, point, or route
- New zone types: butchery, dock, farmland, forest, home, kitchen, pen, storage, tannery, townhall, woodshop
- Zones can be filtered to only accept specific items or entity types (e.g., wheat-only farms, sheep-only pens)
- Right-click air with the Ledger while linked to a village to manage zones
