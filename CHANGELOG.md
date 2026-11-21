# Changelog

## Unreleased

### Added
- Minecraft 26.1, 26.2 and 26.3 support (Fabric and NeoForge)
- Minecraft 1.20.4, 1.20.5 - 1.20.6, 1.21.2 - 1.21.3, 1.21.4, 1.21.5, 1.21.6 - 1.21.8, 1.21.9 - 1.21.10 and 1.21.11 support (Fabric and NeoForge)
  - on 1.20.4 - 1.20.6 the mod uses Fzzy Config 0.5.9 (the newest for those versions)

### Changed
- One codebase for all Minecraft versions and loaders (Stonecutter)
- Architectury API is no longer required
- Fzzy Config 0.7.7, Fabric Loader 0.20.1

## 2.0.4 (2026-04-09; Fabric, Forge, Neoforge; 1.20.1, 1.21.1)

Something Refactored

## 2.0.3 (2026-02-26; Fabric, Forge; 1.20.1)

#### Added
- Added debugLogging config option for troubleshooting
  - Disabled by default
  - Shows effect conversion and cache operations when enabled

#### Fixed
- Fixed compatibility with custom StatusEffectInstance subclasses (Majrusz's Difficulty and similar mods)
- Fixed hasEffect() returning incorrect results during effect application
- Fixed removeStatusEffect() not working in unidirectional mode
- Fixed infinite loops when source equals target effect

#### Performance
- Improved effect ID caching (3 separate caches merged into 1 shared cache)
- Optimized ThreadLocal operations (changed remove() to set(false), cached get() results)
- Added instance-level caching for effect type and amplifier conversions
- Added static lookup cache for hasEffect/getStatusEffect operations
- Cache automatically clears on config reload

## 2.0.2 (2026-02-26; Fabric, Forge; 1.20.1)

#### Performance
- Optimized effect checking with intelligent caching
- Optimized wildcard pattern matching for extraApplyEffectTick (cached matching effects)
- Cached Identifier objects to avoid repeated creation
- Cached resolved target effect in EffectMappingEntry (fallback logic)
- Optimized dangerous pattern validation (removed split() call)
- Eliminated temporary object creation in pattern matching (removed EffectMappingEntry allocation)
- Replaced WeakHashMap with ConcurrentHashMap in all mixins for better cache performance
- Optimized wildcard matching algorithm to use character-by-character comparison instead of String.split()
- Optimized exclusion pattern parsing to use indexOf() instead of split()
- Added per-mapping match cache to avoid repeated pattern matching for same effect IDs
- Optimized getMapping() to use containsKey() check for proper cache hit detection

#### Breaking Changes
- Removed deprecated classes
- Migration from old JSON configs still works automatically

## 2.0.1 (2026-02-24; Fabric, Forge; 1.20.1)

#### Added
- Added autocomplete suggestions for effect fields in config GUI
  - Source effect field now shows all registered effects as you type
  - Target effect field provides suggestions for available effects
  - Extra tick and discard tick fields also support autocomplete

#### Fixed
- Fixed icon.png path in fabric.mod.json (was pointing to wrong location)

## 2.0.0 (2026-02-24; Fabric, Forge; 1.20.1)

#### Added
- Added full Fabric support alongside existing Forge support
- Added in-game GUI for editing effect mappings (powered by fzzyconfig)
- Added amplifier overflow protection (clamped to 0-255 range)
- Added platform-specific default configurations
  - Fabric: empty defaults (users configure their own mappings)
  - Forge: comprehensive wildcards with fallback logic

#### Changed
- **BREAKING:** Migrated from JSON to TOML configuration format
  - Old `config/effectification/effect_mappings.json` will be automatically migrated on first launch
  - Migrated config is backed up to `effect_mappings.json.old`
  - New config location: `config/effectification/effectification.toml`
- Improved GUI for effect mappings with better summaries and tooltips
- Replaced System.out.println with centralized EffectificationLogger

## 1.0.4 (2026-02-23; Forge; 1.20.1)

#### Added
- Added `unidirectional` mode to prevent source mod handlers from triggering
  - Solves overlapping visual effects issue (e.g., cataclysm:stun + alexscaves:stunned causing motion sickness)
  - When enabled, `entity.hasEffect(source)` returns false, only target effect is active
  - Source mod handlers won't trigger, preventing duplicate screen shake/particles
  - Example: `{"sourceEffect": "cataclysm:stun", "targetEffect": "alexscaves:stunned", "unidirectional": true}`
- Added `extraApplyEffectTick` option to call additional effects' visual logic
  - Allows combining mechanics from one effect with visuals from another
  - Target effect provides main logic, extra effects provide additional visuals (particles, sounds)
  - Example: `{"sourceEffect": "cataclysm:stun", "targetEffect": "relics:stun", "extraApplyEffectTick": "alexscaves:stunned"}`
  - Use case: Relics stun mechanics + AlexsCaves stun particles/screen shake
- Added `discardEffectTick` option to block specific effects' applyUpdateEffect()
  - Prevents unwanted side effects while keeping effect active for detection
  - Useful for blocking damage/healing/other logic while preserving effect presence
  - Example: `{"sourceEffect": "*:stun*", "targetEffect": "relics:stun", "discardEffectTick": "cataclysm:stun"}`
  - Use case: Unify stun effects but block Cataclysm's specific logic (e.g., unwanted damage)
- Added default configuration combining all three features for stun effects
  - General stun unification: bidirectional mode with AlexsCaves visuals for all stun effects
  - Discard pattern: blocks applyUpdateEffect for all stun effects except relics and alexscaves
  - Pattern: `*:stun* !*:stun*_immunity !*:stun*_resistance !relics:stun !alexscaves:stunned`
  - Special Cataclysm override: unidirectional mode to prevent event-based handlers
  - Cataclysm uses event-based logic (not applyUpdateEffect), so unidirectional is required
  - Result: All stun effects unified with AlexsCaves visuals, only relics/alexscaves logic active

#### Changed
- Changed mapping priority system: later configs now override earlier configs
  - Exact matches always take priority over wildcards
  - Within wildcards, configs at the bottom override configs at the top
  - Allows specific overrides of wildcard patterns by placing exact matches below

#### Fixed
- Fixed overlapping visual effects when multiple mods have handlers for similar effects

## 1.0.3 (2026-02-22; Forge; 1.20.1)

#### ⚠️ IMPORTANT: Breaking Changes
**🔴 DELETE YOUR OLD CONFIG FILE to use new wildcard patterns!**
- Delete `config/effectification/effect_mappings.json` before updating
- The mod will generate a new config with wildcard patterns automatically
- Old configs without wildcards will still work but won't benefit from new features

#### Added
- Added wildcard pattern support for effect mappings
  - `*:bleeding` - matches any mod's bleeding effect (exact name)
  - `*:bleeding*` - matches effects containing "bleeding" (partial match)
  - `modid:effect*` - matches effects starting with "effect" (SAFE - only with prefix)
  - `*:bleeding* !*:bleeding_immunity` - with exclusions (use `!` to exclude)
  - Exact matches take priority over wildcards
- Added protection against dangerous patterns (`*:*` and `namespace:*`, even with exclusions)
- Added exclusion support using `!` separator in sourceEffect

#### Fixed
- Fixed StackOverflowError caused by regex pattern matching in wildcard system
- Fixed potential StackOverflowError caused by registry lookups in mixins
- Replaced regex-based matching with simple string matching to avoid recursion issues
- Added recursion protection with ThreadLocal flags in all mixins
- Added WeakHashMap caching for effect IDs to avoid repeated registry calls

#### Changed
- Simplified default configuration using wildcard patterns with exclusions
- Reduced number of default mappings using wildcards

## 1.0.2 (2026-02-22; Forge; 1.20.1)

#### Removed
- Removed `bidirectional` mode (effects are fully replaced, no need for reverse conversion)
- Removed `fullReplacement` mode (redundant, effects are replaced by default)

#### Changed
- Config migration automatically removes obsolete fields from old configs

## 1.0.1 (2026-02-22; Forge; 1.20.1)

#### Added
- Added config versioning system with automatic migration
- New mappings are automatically added on mod updates without losing user changes
- Added lava_vision effect mapping (netherdepthsupgrade → alexsmobs)

#### Changed
- Config file format now includes version field for better update management

## 1.0.0 (2026-02-18; Forge; 1.20.1)

- Initial release
