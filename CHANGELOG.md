# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/SoSly/VillageWorks/tree/1.20.1)

### Added
- A new `Villager` entity that is the basis of the mod
- Hunger system for villagers using `LivingEntityFoodData`
- Hunger awareness system for villagers (automatically detects hunger states for AI behaviors)
- Basic inventory system for villagers (can hold one food item via right-click)
- `/vw assign` command to set villager homes
- `/vw exhaust` command to add exhaustion to villagers for testing
- `/vw hunger` command to display villager food stats
