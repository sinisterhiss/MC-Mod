# Unified Storage

Fabric mod prototype targeting Minecraft 1.21.1 / Java 21.

## Behavior in v0.3

- A chest, trapped chest, or barrel **placed by a player after the mod is installed** becomes a Universal Storage terminal.
- Naturally generated / already-existing containers remain ordinary Minecraft loot containers.
- Opening any marked terminal opens the same persistent, per-player storage pool from anywhere and in any dimension.
- Universal Storage is effectively unbounded and displayed 54 stacks at a time with page buttons.
- If a ground-item pickup does not fit in the player inventory, the remainder is automatically put in Universal Storage.
- Universal Storage has Previous / Next page, Sort, Deposit All, and Loot All controls.
- Breaking a marked chest/barrel removes its terminal marker but does **not** delete Universal Storage.

## v0.3: 999-count stacks in the player inventory too

The player inventory and Universal Storage now both use a **999 item maximum per stack**.

The rule is **exact identity**, not merely "same item type":

- Same item + same complete component data => may stack.
- Different durability => **not identical** and will not merge.
- Different enchantments or enchantment levels => **not identical**.
- Different custom name => **not identical**.
- Different potion contents, trim, stored container contents, custom data, etc. => **not identical**.

The implementation uses Minecraft's `ItemStack.areItemsAndComponentsEqual`, which compares the item and complete component state while ignoring only the count.

This means normally-unstackable items such as tools, weapons, armor, enchanted books, potions, buckets, and similar items can stack when their complete state is identical.

### Durability behavior

Damageable items need one extra rule because Minecraft normally stores durability once per entire ItemStack. When a player uses one item from a stack of identical damageable tools/weapons, v0.3 first separates one item from the stack. Vanilla then applies durability to that one item, while the untouched copies remain at their old durability and therefore become a different stack.

If there is no free player-inventory space for the untouched remainder during that split, the untouched remainder is moved to Universal Storage rather than being lost.

This covers vanilla durability changes. A modded item that mutates unusual custom components in-place without using Minecraft's normal durability path may need a compatibility hook in a later version.

## Saving 999-count stacks

Minecraft 1.21.1's normal ItemStack NBT codec only accepts counts up to 99. v0.3 intercepts ItemStack NBT save/load:

- counts 1-99 use vanilla serialization unchanged;
- counts 100-999 are encoded with a vanilla-safe count plus a `UnifiedStorageCount` integer containing the real count;
- loading restores the real count.

This is used for player inventory persistence as well as Universal Storage.

## Current edge cases

- Do not intentionally merge a newly placed terminal chest with an unmarked natural chest into one double chest yet. A later revision should explicitly prevent mixed double chests.
- Hoppers and other automation still interact with the physical chest/barrel block entity, not Universal Storage.
- Naturally generated / unmarked chests remain ordinary loot containers; the 999-player-stack feature does not turn those chests into Universal Storage.
- Special-purpose GUI slots that intentionally accept only one item (armor slots, enchanting input, etc.) can still enforce their own slot-specific limit.
- A modded item that changes custom components in-place may require a specific split-before-mutation compatibility rule.

## Build

GitHub Actions builds this project on every push using Java 21 and Gradle.

Locally:

```
gradle build
```

The mod jar will be in `build/libs/`.

## Next features

- Search/filter box.
- Loot All button on unmarked/natural chest GUIs.
- Prevent natural + terminal mixed double chests.
- Crafting-table recipes that consume player inventory first, Universal Storage second.
- Optional shared/team storage for multiplayer.
- Optional overflow action-bar message.
