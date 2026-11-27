![Effectification](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/banner.webp)

<p align="center">
<a href="https://www.curseforge.com/minecraft/mc-mods/effectification"><img src="https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/button-curseforge.webp" alt="CurseForge" height="40"></a>
<a href="https://modrinth.com/mod/effectification"><img src="https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/button-modrinth.webp" alt="Modrinth" height="40"></a>
<a href="https://github.com/UtherHel/effectification"><img src="https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/button-github.webp" alt="GitHub" height="40"></a>
</p>

**Bleeding is bleeding.** Three mods add Bleeding, two add Stun, and each checks only its own: an immunity from one does nothing against another, and two of them stack on the same target. Effectification turns an effect of one mod into the matching effect of another, so every mod sees one and the same effect. Minecraft Java 1.20.1 to 26.3: Fabric 1.20.1 and 1.20.4 - 26.3, Forge 1.20.1, NeoForge 1.20.4 - 1.20.6, 1.21.1 - 1.21.5 and 1.21.7 - 26.3. Needs Fzzy Config (and Fabric API on Fabric). Install it on the server and on every client.

![Features](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/title-features.webp)

- **One effect for all mods.** A mapping applies the target effect wherever the source would be applied, and anything that looks for the source finds the target: immunities, cures and conditions of both mods see the same effect.
- **Wildcards with exceptions.** `*:bleeding* !*:bleeding*_immunity` catches the Bleeding of every mod but leaves the immunities alone; `somemod:*` takes every effect of one mod.
- **Fallbacks.** Name several targets, separated by commas: the first one that is installed is used.
- **Strength.** `amplifierMultiplier` scales the level on the way (kept within 0-255).
- **Fine control.** Change only the name and icon (`onlyVisualChange`), stop the source mod's own handlers (`unidirectional`), keep another effect's tick running for its visuals (`extraApplyEffectTick`) or silence an effect's tick (`discardEffectTick`).
- **Static or dynamic.** Dynamic mappings convert while playing and apply as soon as you save. Static mappings are set at startup and need a restart: an id that no installed mod registers (a mod you removed, an effect a command or data pack still uses) becomes the target effect, so old worlds and commands keep working (on Forge 1.20.1 old worlds only: commands there need the target's own id).
- **Tells you what is wrong.** At startup and after every change the log lists mappings with a typo in an effect id, and one summary line counts the active ones and those waiting for a mod that is not installed. In the config screen the tooltip of a mapping with a problem says what it is, in red.
- **An editor in the game,** with every registered effect suggested as you type: Mod Menu on Fabric, the mod list on Forge and NeoForge. The server's mappings are sent to every player.
- **Ready-made mappings** on Forge and NeoForge for Bleeding, Stun, Freeze, Chill, Immolate, Confusion, Knowledge and Sapped, and for the pairs that overlap in Majrusz's Difficulty, Apothic Attributes, Valoria, Relics, Alex's Caves, Alex's Mobs, Mowzie's Mobs, More RPG Classes, Iron's Spells, Goety and Nether Depths Upgrade. Fabric starts empty.

![How to use](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/title-how-to-use.webp)

Install it and play: with the built-in mappings a Stun from Goety, Alex's Caves or any other mod is applied as Relics' Stun, and Relics' immunity protects from all of them.

To add a mapping, open the config screen (Mod Menu on Fabric, the mod list on Forge and NeoForge) and add an entry under Dynamic Mappings, or edit `config/effectification/effectification.toml`:

```toml
[[dynamicMappings]]
sourceEffect = "*:stun* !*:stun*_resistance !*:stun*_immunity"
targetEffect = "relics:stun, alexscaves:stunned"
amplifierMultiplier = 1.0
onlyVisualChange = false
unidirectional = false
extraApplyEffectTick = ""
discardEffectTick = ""
```

Every Stun except the resistances and immunities becomes Relics' Stun, or Alex's Caves' Stunned when Relics is not installed.

![Settings](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/title-settings.webp)

**Every mapping**, static or dynamic, has the same fields:

| Field | Default | What it does |
| --- | --- | --- |
| `sourceEffect` | - | the effect to replace: an ID or a pattern with `*`, then `!pattern` exceptions after a space |
| `targetEffect` | - | the effect to use instead; several, separated by commas, are fallbacks in order |
| `amplifierMultiplier` | 1.0 | the level is multiplied by it |
| `onlyVisualChange` | false | show the target's name and icon, keep the source's level and behaviour |
| `unidirectional` | false | the source mod's own handlers no longer react to the effect |
| `extraApplyEffectTick` | empty | effects whose tick also runs while the target is active, for their visuals |
| `discardEffectTick` | empty | effects whose tick is blocked; patterns allowed |

`staticMappings` need a restart, `dynamicMappings` apply when saved. A static mapping whose source is a registered effect works like a dynamic one. `*:*` is not allowed as a source: a pattern needs a fixed part.

`debug.logLevel` controls the log: `NONE`, `ERRORS`, `WARNINGS` (the default: also mapping problems, such as an effect id with a typo) or `ALL` (also every conversion, for troubleshooting). Changing the server's config takes permission level 2.

**Same idea, for attributes:** [Attributification](https://modrinth.com/mod/attributification).

<!-- promo:start -->
<!-- More from UtherHel: promo cards of other mods go here, two per row, as in the other pages. -->
<!-- promo:end -->

![License](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/title-license.webp)

MIT License: free to use, modify, share and include in modpacks, as long as the copyright notice stays with it. See [LICENSE](https://github.com/UtherHel/effectification/blob/main/LICENSE).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

**More from UtherHel:** [GitHub](https://github.com/UtherHel/effectification) (source and issues)

![](https://raw.githubusercontent.com/UtherHel/effectification/main/docs/desc/divider.webp)