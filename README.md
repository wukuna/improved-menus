# Improved Menus

Independent RuneLite menu protections, item rules, colors and profiles in one searchable panel.

## Features

- **Inventory:** protect potion Empty, hide selected item actions by name, and support item-on-item Use targets.
- **Bank:** independently hide inventory Wield, Wear, Drink and Eat while the bank is open. Deposit and withdraw actions are preserved.
- **Examine:** separate inventory, ground-item, NPC and scenery switches, with options to keep Examine when it is the only action.
- **Tools:** hide tree Chop/Cut or rock Mine when the required axe or pickaxe is neither carried nor equipped. Unknown inventory state leaves actions available.
- **House:** hide building actions outside building mode, plus unnecessary Mahogany Homes Remove actions. Other furniture actions stay available.
- **Buttons:** normal, right-click-only or hidden modes for Auto Retaliate, XP Drops, bank placeholders and Report. Right-click-only checks where the click began; it does not merely move the action down the list.
- **Presentation:** action colors, wildcard mappings, submenu support, a Shift-menu color picker/reset, optional ground-loot icons and additional-option counts.
- **Shared editor:** search, name/context exceptions, diagnostics showing the before/after entry counts and reasons, named profiles, selective reset, twenty-step undo, and validated import/export previews.

Protection switches start off. Enable the features you want in the sidebar or RuneLite settings. Button modes start at Normal. Hold **Ctrl before opening a menu** to reveal options hidden by this plugin. The reveal modifier can be changed to Shift, Alt or None. It does not consume keys or recreate options removed by another plugin.

## Item rules and exceptions

Use one `action,item name` rule per line. Matching ignores case and formatting tags. `*` matches any text; other characters are literal. Lines starting with `#` are comments.

```text
Drop,Rune*
[bank] Eat,Shark
[ground] Take,Coins
Use,* -> Torn clue scroll*
```

Optional contexts are `[inventory]`, `[bank]`, `[ground]` and `[all]`. Untagged rules apply to supported item entries in all contexts. Rules never filter player actions, NPC combat actions or item/spell use on actors.

Exceptions use one name per line, with the same optional contexts and wildcard matching. For example, `[ground] Coins` keeps ground-item Coins options while leaving inventory rules active. Exceptions override the filtering modules; button protection has its own settings.

Menu colors use `target:action:#RRGGBB`, separated by semicolons:

```text
*:Take:#88cc88;Coins:Take:#ffcc33
```

Exact color mappings take precedence over wildcard mappings. Hold Shift when opening a menu to use the local color editor. Reset removes that entry's exact mapping; a broader wildcard can still apply.

The editor accepts at most 200 rules per field. Invalid manual configuration edits disable that rule field rather than retaining old invisible rules. Profile imports validate every setting before applying changes. Imports cannot modify other plugins' settings. Existing-setting import reads explicitly saved settings from the supported plugins listed below and leaves those originals intact. Missing settings and source-plugin enablement are not inferred.

## Other mods / sources

This project combines features from, and credits, these plugins. Links identify the source versions reviewed for this release. Adapted code/catalogs and complete licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and distributed with the plugin.

| Source plugin | Features included |
|---|---|
| [No Examine](https://github.com/Skretzo/runelite-plugins/tree/ea506dbbfd2aa70dd024f2fc7b761786e2ced91a) | Category-specific Examine removal and keeping lone Examine |
| [Not Empty](https://github.com/dekvall/runelite-external-plugins/tree/0c1c5cf2a8588ac3b017e7026c393d2eee4980c3) | Dosed-item Empty protection |
| [No Misclicks In Bank](https://github.com/Welkon1/No-Misclicks-In-Bank/tree/6f76aa102a04525bdce2424b57b4ad899ba82e8b) | Independent Wield, Wear, Drink and Eat bank guards |
| [Item Option Hider](https://github.com/kekingbd/item-option-hider/tree/5375d699a8661c157c7e26b699a9c46401237c7e) | Item action/name rules, wildcards and item-on-item targets |
| [Menu Colorizer](https://github.com/dgalambos/menu-colorizer/tree/3e73b989615aecb2813f0b74bb9ecdf5ba5451ac) | Cosmetic action colors, interactive picker/reset and submenus |
| [Tool Required](https://github.com/Unmoon/tool-required/tree/031e4986c2235f58888cf158666a02f3497cac60) | Axe/pickaxe requirements and tool catalog |
| [Home Improvement](https://github.com/Mike-U5/rl-plugin-home-improvement/tree/6b7cca56d95d794bae252f5f7b58b8d2d5f6027e) | House building-mode cleanup and Mahogany Homes catalog |
| [Misclick Guard](https://github.com/willzero123/misclick-guard/tree/a49e33e7576dbf60ec3caa39be4f470b6be71f47) | Button matching, click-origin protection and Cancel fallback |
| [Ground Loot Icons](https://github.com/Fiffers/ground-loot-icons/tree/8e3e529f27aef3b7a5323ef8fef4f565d8b00be2) | Take-menu item icons and row geometry |
| [Mouseover Options+](https://github.com/Petzep/mouseover-options/tree/872f6bf6a0817e5d6292749c2f3d4ae8a8eb9600) | Additional-option count concept |

Users of the features in this table can consolidate them here after checking their settings. This is feature coverage, not a claim that every setting in each source plugin has been reproduced. No Examine's global Cancel/Walk/Set-heading removal, Mouseover Options+'s full hover text controls, and Menu Colorizer's configurable default picker color are not included. Hold-to-reveal is shared rather than Examine-only. Mahogany Homes cleanup preserves non-Remove actions.

Keep RuneLite's Menu Entry Swapper for swaps and specialist plugins for their remaining features. Disable overlapping filters/colors before comparing results: this plugin cannot reveal options another plugin has removed.

## Development

Java 11 is used for the build. Run `gradlew.bat test` for automated checks or `gradlew.bat run` to launch the development client. Tests cover filtering boundaries, order/callback preservation, contexts, profiles/import validation, click origin, tooltip ownership, reveal and cosmetic restoration. No additional production runtime dependency is required.

A short live smoke check is still needed for real menu rendering: try a dosed potion and bank item, one protected button through both click paths, a tree with/without an axe, a house menu outside/inside building mode, a colored submenu, and ground loot icons. Disable/re-enable once while a menu is open. Extensive repeated testing is not required, but automated checks do not verify actual in-game rendering.
