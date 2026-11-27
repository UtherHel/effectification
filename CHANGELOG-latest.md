### Added
- Minecraft 26.1, 26.2 and 26.3 support (Fabric and NeoForge)

### Changed
- The log tells you what is wrong: at startup and after every change, mappings with a typo in an effect id are listed as warnings, and one line counts the active mappings and those waiting for a mod that is not installed. In the config screen the tooltip of a mapping with a problem names it, in red
- The default log level is WARNINGS (mapping problems are shown); messages from effects applied every tick are logged once instead of every time
- `somemod:*` is allowed as a source or discard pattern (every effect of one mod); only `*:*` is refused
- Clearer names and descriptions in the config screen and in the config file, in English and Russian
- One codebase for all Minecraft versions and loaders (Stonecutter)
- Architectury API is no longer required
- Fzzy Config 0.7.7, Fabric Loader 0.20.1

### Fixed
- A static mapping whose source is a registered effect now converts it; before, the effect stayed the source while checks for it looked for the target
- The amplifier of a converted effect follows upgrades: an effect raised to a higher level kept the old converted level
- `extraApplyEffectTick` works when the target differs from the source (it only ran for mappings onto the same effect), and runs on the server only, as effect ticks do
- The name and level of a converted effect are right even before anything else asked for the effect (tooltips, the HUD)
- Rare crashes or wrong results in single player from caches shared by the server and the client thread
- Static mappings work on Forge 1.20.1: the id of an effect that no mod registers goes to the target in Forge's own registry, so saved effects of a removed mod load as the target (there, commands such as `/effect` still need the target's own id). The startup report of mapping problems runs on Forge 1.20.1 as well
