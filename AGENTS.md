# Agents guide

Use this as the house rules for AI contributions.

- Stack: Fabric on modern Minecraft (aim for 1.21.1+; update to the latest Fabric API when available). Keep code Yarn-friendly.
- Design: composition over inheritance; passives/abilities are pluggable components; gem definitions live in data/registries so adding gems is straightforward.
- Specs: source-of-truth is `checklist.md` for abilities, energy, items, and unlock rules.
- Unlock rules: level 1 unlocks all passives; abilities unlock in order at levels 2-4; any remaining abilities unlock together at level 5; levels 6-10 are buffer only.
- Energy rules: follow `checklist.md` (Legendary→Broken ladder, start at 3 energy, kill gains, death losses, Legendary +5 glint and upgrade-item drops; abilities scale with energy: 0-1 none, 2-4 first energy-1 abilities, 5+ all abilities).
- Performance: avoid per-tick heavy scans; cache player state; keep abilities server-authoritative; cleanly register/unregister listeners; add tests or benchmarks around hot paths and state transitions.
