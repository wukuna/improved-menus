# Improved Right-Clicks

Customize right-click menus with independent misclick protections, item rules, action colors, ground-item menu icons and a configurable extra-options indicator. Choose independent protections for items, bank actions and interface buttons, hide unwanted options with item rules, and color actions so useful choices stand out. Save named profiles, preview imports and undo changes made through the side panel tools.

Protection switches start off. Enable the features you want in RuneLite's Improved Right-Clicks settings, then use the side panel for profiles, import/export and diagnostics. Hold Ctrl before opening a menu to reveal options hidden by this plugin; the reveal modifier is configurable.

Previously **Menu Workshop**. The name change keeps your existing settings and profiles. A one-time chatbox notice explains the rename after login.

[Features](#features) · [Getting started](#getting-started) · [Usage](#usage) · [Saved settings](#saved-settings) · [Compatibility](#compatibility) · [Related plugins and sources](#related-plugins-and-sources) · [Development](#development) · [Support](#support) · [License and attribution](#license-and-attribution)

## Features

- **Inventory:** protect potion Empty, hide selected item actions by name, and support item-on-item Use targets.
- **Bank:** independently hide inventory Wield, Wear, Drink and Eat while the bank is open. Deposit and withdraw actions are preserved.
- **Examine:** separate inventory, ground-item, NPC and scenery switches, with options to keep Examine when it is the only action.
- **Menu cleanup:** independently hide Cancel, Walk here and Set heading in opened right-click menus. Normal left-click walking/heading and button-protection fallback remain available. Cancel is retained when hiding it would leave an empty menu.
- **Target protection:** hide item Use on players, with a configurable allowed-item list; spell casts and other player actions are preserved. Hide Telekinetic Grab on NPCs while preserving ground-item casts, other spells, known useful targets and custom NPC-name exceptions.
- **Tools:** hide tree Chop/Cut or rock Mine when the required axe or pickaxe is neither carried nor equipped. Unknown inventory state leaves actions available.
- **House:** hide building actions outside building mode, plus unnecessary Mahogany Homes Remove actions. Other furniture actions stay available.
- **Buttons:** normal, right-click-only or hidden modes for Auto Retaliate, XP Drops, bank placeholders and Report. Right-click-only checks where the click began; it does not merely move the action down the list.
- **Presentation:** action colors, wildcard mappings, submenu support, a Shift-menu color picker/reset with configurable default color, optional ground-loot icons and additional-option counts. Counts support a minimum threshold, including/excluding Examine, showing/hiding counts on interface widgets, and a configurable text color.
- **Settings:** configure every feature in RuneLite's normal Improved Right-Clicks settings, grouped into collapsible sections. **Hide options** groups Walk here, Cancel, Set heading, Use on Player, Telegrab NPCs, potion Empty and Examine controls. Bank stays last.
- **Side panel tools:** a direct Open Plugin Settings shortcut, named profiles, twenty-step undo, validated import/export previews, and collapsible diagnostics showing the before/after entry counts and reasons. Closing a dialog leaves the last operation status unchanged; applying identical settings reports No changes. Settings are not duplicated in the side panel. Undo restores only settings changed by a profile/import/editor operation, leaving later manual changes alone. Operations that change nothing do not consume an undo step; ordinary RuneLite settings use RuneLite's normal controls.

Protection switches start off. Enable the features you want in RuneLite's Improved Right-Clicks settings. Button modes start at Normal. Hold **Ctrl before opening a menu** to reveal options hidden by this plugin. The reveal modifier can be changed to Shift, Alt or None. It does not consume keys or recreate options removed by another plugin.

Enable **Hide side panel** under General to remove the Improved Right-Clicks toolbar button. Menu features and the Shift-menu color editor remain active. Turn it off to access profiles, import/export, undo and diagnostics again.

## Getting started

1. Open **Improved Right-Clicks** in RuneLite's plugin settings and enable the protections or presentation options you want.
2. Use the side panel for profiles, import/export, undo and diagnostics.
3. Hold the configured reveal modifier before opening a menu to temporarily show options this plugin hid.


## Usage

### Item rules and exceptions

Use one `action,item name` rule per line. Matching ignores case and formatting tags. `*` matches any text; other characters are literal. Lines starting with `#` are comments.

```text
Drop,Rune*
[bank] Eat,Shark
[ground] Take,Coins
Use,* -> Torn clue scroll*
```

Optional contexts are `[inventory]`, `[bank]`, `[ground]` and `[all]`. Untagged rules apply to supported item entries in all contexts. Rules never filter player actions, NPC combat actions or item/spell use on actors.

Exceptions use one name per line, with the same optional contexts and wildcard matching. For example, `[ground] Coins` keeps ground-item Coins options while leaving inventory rules active. These exceptions override item, Examine, tool and house filtering; button protection and target protection have their own settings.

**No Empty on potions** hides Empty on dosed inventory potions while retaining Drink. It starts off, like the other protection switches.

**No Use on Player** allows common quest, Barbarian Assault and cooperation items by default. Edit **Allow items on players** with exact item names separated by commas. **Don't Telegrab NPCs** always permits the Maze Guardian, King's Ransom Guard, Wyrmscraig hunter goats, Sailing creature corpses and lost Sailing crates. Add other exact names under **Allow Telegrab NPC names**. These controls do not support arbitrary actor/combat filtering. Missing selection or target information leaves the option available. The shared reveal modifier bypasses both protections.

Menu colors use `target:action:#RRGGBB`, separated by semicolons:

```text
*:Take:#88cc88;Coins:Take:#ffcc33
```

Exact color mappings take precedence over wildcard mappings. Hold Shift when opening a menu to use the local color editor. Reset removes that entry's exact mapping; a broader wildcard can still apply.

**Yellow hover highlight** is enabled by default: a colored main-menu option uses the native yellow highlight while hovered, then returns to its configured color. Submenu entries retain their configured colors because RuneLite does not expose which submenu is open for reliable hover detection. Disable it to retain the configured color on hover. Item/name colors and image tags are preserved.

**Default picker color** applies when an entry has no matching color mapping. **Option count text color** changes the hover count. Both colors use `#RRGGBB` in RuneLite settings. The Shift menu color editor provides an interactive picker for menu entries. **Minimum extra options** accepts 1–500. Counts exclude Cancel, Walk here and local editor actions; **Count Examine** and **Show count on interface widgets** control the remaining categories. These settings are included in profiles, undo and imports.

The editor accepts at most 200 rules per field. Invalid manual configuration edits disable that rule field rather than retaining old invisible rules. Profile imports validate every setting before applying changes. Imports cannot modify other plugins' settings. Existing-setting import reads explicitly saved settings from the supported plugins listed below and leaves those originals intact. Missing settings and source-plugin enablement are not inferred.

### Ground-menu icons and Loot Filters

In RuneLite's **Improved Right-Clicks** settings, open **Presentation**, enable **Ground loot icons**, and choose **Native**, **Custom**, or **Both** under **Icon source**. All icon, visibility and color options stay in plugin settings. The side panel contains profiles, import/export, undo, a settings shortcut and optional diagnostics.

- **Native** shows the item's own picture and remains the default.
- **Custom** shows the selected custom icon. Hidden, unassigned or unavailable icons remain absent.
- **Both** shows custom and native icons together, avoiding duplicate Current Item icons. The native picture remains when no custom icon is available, unless **Both: respect filter visibility** is enabled.

Enable **Loot Filters compatibility** with regular Loot Filters running, then choose Custom or Both. Improved Right-Clicks reads its selected saved filter and relevant settings, evaluates the rules locally, and loads its selected icons. No manual upload or modified Loot Filters build is needed. Existing manual assignments remain explicit overrides; previously imported filter backups remain saved and can supply icons with compatibility off.

`CurrentItem()`, `Item(id)`, `Sprite(id,index)` and `File("name.png")` icons are supported. File icons are loaded from Loot Filters' icons folder, cached and fitted to the menu. Loading can finish while the menu remains open. Original files and Loot Filters settings are never modified.

**Use Loot Filters in menu** optionally applies the evaluated Take-row sort priority while preserving existing deprioritization and non-loot rows. **Use Loot Filters menu colors** is a separate opt-in for item-name colors when that menu switch is enabled. Hidden-row colors remain as regular Loot Filters set them. Both switches start off and preserve saved action colors and normal left-click actions.

Selected filters/settings follow the current RuneLite profile. Saved filter changes are checked approximately every two seconds. Highlighted/hidden lists precede script rules; compact mode and the final `hidden` / `hideOverlay` values are respected. Temporary ground-overlay toggles and camera visibility do not hide menu icons. When identical stacks resolve differently, no custom icon is chosen arbitrarily; Both retains its native picture unless its visibility option suppresses it. Submenus retain their original text.

This reads saved state rather than Loot Filters' in-memory program: reload externally edited scripts in Loot Filters too. Bundled default filters match the inspected regular release and require updates when upstream changes them. **Show diagnostics** in the side panel includes source and loading information. See [compatibility details](LOCAL-COMPATIBILITY.md) for limits.

The RS2F parser, rule evaluation helpers, preamble and three default filters are adapted from [riktenx/loot-filters at 993fd054](https://github.com/riktenx/loot-filters/tree/993fd054e08d42a77d3415acbb9ac7c2797726ed), under its [BSD 2-Clause license](src/main/resources/com/improvedmenus/loot-filters-LICENSE.txt). This integration changes menu presentation only; it does not render ground overlays or play sounds.

### Additional option indicator

Under Presentation, **Hide player option counts** excludes player actions while retaining loot and object actions underneath them. **Hide counts when Walk here** suppresses deprioritized NPC/object counts when walking is preferred; ground items and widgets remain eligible. These switches affect only the hover indicator, not the menu entries. Both start off.

**Option count anchor** offers Below left (the existing default), Below right, Above left and Above right relative to the tooltip stack. RuneLite still controls the stack position relative to the cursor. All options are saved in profiles and support import/export and undo.

## Saved settings

Settings and named profiles retain the existing `improvedmenus` storage keys. Renaming the plugin or repository does not reset them. Profiles, validated imports and the side panel's Undo tools manage plugin settings; previously saved custom icon assignments and imported filter backups remain intact after removing the sidebar editor. Imports leave other plugins' original settings and files intact.

## Compatibility

Users of the features in this table can consolidate them here after checking their settings. This is feature coverage, not a claim that every setting in each source plugin has been reproduced. Cancel/Walk/Set-heading cleanup applies only to opened menus. Option counts use a standard hover tooltip rather than Mouseover Options+'s custom badge layout. Useful Telegrab targets are always allowed rather than individually switchable. Hold-to-reveal is shared rather than Examine-only. Mahogany Homes cleanup preserves non-Remove actions.

Keep RuneLite's Menu Entry Swapper for swaps and specialist plugins for their remaining features. Disable overlapping filters/colors before comparing results: this plugin cannot reveal options another plugin has removed.

Menu Entry Swapper's Shift-right-click customization remains available with Examine hiding and menu colors enabled. Improved Right-Clicks keeps its native Examine lookup entries unchanged while building that menu, then applies the configured cleanup after the swap controls have been added.

Optional Loot Filters compatibility reads the selected saved filter from the regular plugin. Configure it under Presentation as described above. Icons decorate menu entries; they do not change ground labels, beams or sounds.

## Related plugins and sources

This project combines features from, and credits, these plugins. Links identify the source versions reviewed for this release. Adapted code/catalogs and complete licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and distributed with the plugin.

| Project | Relationship | Relevant features and limits |
| --- | --- | --- |
| [No Examine](https://github.com/Skretzo/runelite-plugins/tree/ea506dbbfd2aa70dd024f2fc7b761786e2ced91a) | Source / feature overlap | Category-specific Examine removal, keeping lone Examine, right-click Cancel/Walk here/Set heading cleanup |
| [Not Empty](https://github.com/dekvall/runelite-external-plugins/tree/0c1c5cf2a8588ac3b017e7026c393d2eee4980c3) | Source / feature overlap | Dosed-item Empty protection |
| [No Misclicks In Bank](https://github.com/Welkon1/No-Misclicks-In-Bank/tree/6f76aa102a04525bdce2424b57b4ad899ba82e8b) | Source / feature overlap | Independent Wield, Wear, Drink and Eat bank guards |
| [Item Option Hider](https://github.com/kekingbd/item-option-hider/tree/5375d699a8661c157c7e26b699a9c46401237c7e) | Source / feature overlap | Item action/name rules, wildcards and item-on-item targets |
| [Menu Colorizer](https://github.com/dgalambos/menu-colorizer/tree/3e73b989615aecb2813f0b74bb9ecdf5ba5451ac) | Source / feature overlap | Cosmetic action colors, interactive picker/reset and submenus |
| [Tool Required](https://github.com/Unmoon/tool-required/tree/031e4986c2235f58888cf158666a02f3497cac60) | Source / feature overlap | Axe/pickaxe requirements and tool catalog |
| [Home Improvement](https://github.com/Mike-U5/rl-plugin-home-improvement/tree/6b7cca56d95d794bae252f5f7b58b8d2d5f6027e) | Source / feature overlap | House building-mode cleanup and Mahogany Homes catalog |
| [Misclick Guard](https://github.com/willzero123/misclick-guard/tree/a49e33e7576dbf60ec3caa39be4f470b6be71f47) | Source / feature overlap | Button matching, click-origin protection and Cancel fallback |
| [Ground Loot Icons](https://github.com/Fiffers/ground-loot-icons/tree/8e3e529f27aef3b7a5323ef8fef4f565d8b00be2) | Source / feature overlap | Take-menu item icons and row geometry |
| [Mouseover Options+](https://github.com/Petzep/mouseover-options/tree/872f6bf6a0817e5d6292749c2f3d4ae8a8eb9600) | Source / feature overlap | Additional-option counts, minimum threshold, Examine/widget switches and text color |
| [No Use Player](https://github.com/oohwooh/no-use-players/tree/e76b5612cea2f9fe68b0ba6cecd2bf11b1a5d656) | Source / feature overlap | Item-only player-use protection and default allowed items |
| [Don't Telegrab NPCs](https://github.com/ldavid432/dont-telekinetic-grab-npcs/tree/71b6675364d67a65eb0023667c42863eeb0e3ea7) | Source / feature overlap | Telegrab-specific NPC protection and useful-target catalog |

## Development

Java 11 is used for the build. Run `gradlew.bat test` for automated checks or `gradlew.bat run` to launch the development client. Tests cover filtering boundaries, order/callback preservation, contexts, profiles/import validation, click origin, tooltip ownership, reveal and cosmetic restoration. See the build configuration and third-party notices for dependencies and licensing.

A short live smoke check is still needed for real menu rendering: try a dosed potion and bank item, one protected button through both click paths, a tree with/without an axe, a house menu outside/inside building mode, a colored submenu, and ground loot icons. Disable/re-enable once while a menu is open. Extensive repeated testing is not required, but automated checks do not verify actual in-game rendering.

Ground loot icons decorate the main right-click menu only. Submenus keep their original target text because RuneLite does not expose whether a submenu is open for reliable icon positioning. Reopening a menu and disabling icons restore the target text owned by Improved Right-Clicks.

Tool guards leave the knife-cut Temple Trekking swamp vines and quest Cave entrance interactions available. Ordinary axe and pickaxe protections remain independent.

## Support

[Report an issue or suggest a feature](https://github.com/wukuna/improved-right-clicks/issues). Include the affected setting, steps to reproduce, other menu plugins and a screenshot when useful. For imported icons, include a small example filter with private details removed.

## License and attribution

Improved Right-Clicks uses the [BSD 2-Clause license](LICENSE). Adapted projects, bundled RS2F components and their licenses are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the resource notices. Source attribution is separate from feature compatibility.
