# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/SoSly/VillageWorks/tree/1.20.1)

### Added
- A new `Villager` entity that is the basis of the mod
- Hunger system for villagers using `LivingEntityFoodData`
- Hunger awareness system for villagers (automatically detects hunger states for AI behaviors)
- Automatic eating behavior for hungry villagers (properly consumes food from inventory when hungry with animation)
- Basic inventory system for villagers (can hold one food item via right-click)
- `/vw assign` command to set villager homes
- `/vw exhaust` command to add exhaustion to villagers for testing
- `/vw hunger` command to display villager food stats
- `/vw village create <name> [squadius]` command to create villages with configurable boundaries
- `/vw village remove <name>` command to remove villages by name
- `/vw village list` command to display all villages in current dimension
- `/vw village info [name|uuid]` command to show village information and boundaries
- Custom `villageworks:storage_containers` block tag enables datapack creators to define which blocks villagers recognize as storage containers
- Town Hall block that automatically creates a village when placed
- Villages are automatically named after the player who places the Town Hall
- Villages require a minimum distance of 30 chunks between each other
- Zone system for defining functional areas within villages
- Four zone shapes: AABB (rectangular), Radius (circular), BlockPos (single block), and Path (connected points)
- Zone types: STORAGE (finds chests/barrels), TOWNHALL (finds town hall blocks), HOME (finds beds), NONE (no POI scanning)
- `/vw zone create` commands for all zone shapes with tab completion for zone types
- `/vw zone delete <zoneUUID>` command to remove zones with UUID tab completion
- `/vw zone rename <zoneUUID> <name>` command to rename zones
- `/vw zone info <zoneUUID>` command showing zone bounds, type, and discovered POIs with block names
- `/vw zone list` command to display all zones in a village
- `/vw zone path add/clear` commands for managing path zone points
- Tab completion for village UUIDs and zone UUIDs in all zone commands
- Support for relative coordinates (`~ ~ ~`) in zone creation commands
- Automatic POI (Point of Interest) detection and caching within zones
