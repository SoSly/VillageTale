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
    - Cook (creates foods from recipes)
    - Farmer (tills soil, plants seeds, harvests crops)
    - Forester (cuts trees, plants trees)
    - Herder (manages animals, brings them to pens)

**Villages & Zones**
- Ledger item - craftable tool for managing villages
- Town Hall block - establish your village (automatically named after you)
- Bell recipe added
- Villages must be at least 30 chunks apart
- Zone system for defining areas within villages
- Four zone shapes available: box, cylinder, point, or route
- New zone types: farmland, forest, home, kitchen, pen, storage, townhall
- Zones can be filtered to only accept specific items or entity types (e.g., wheat-only farms, sheep-only pens)
