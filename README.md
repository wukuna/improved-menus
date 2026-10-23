# Improved Right-Clicks

Make RuneLite menus easier to scan and reduce unwanted clicks. Choose independent protections for items, bank actions and interface buttons, hide unwanted options with item rules, and color actions so useful choices stand out. Save named profiles, preview imports and undo changes made through the side panel tools.

Protection switches start off. Enable the features you want in RuneLite's Improved Right-Clicks settings, then use the side panel for profiles, import/export and diagnostics. Hold Ctrl before opening a menu to reveal options hidden by this plugin; the reveal modifier is configurable.

Previously **Menu Workshop**. The name change keeps your existing settings and profiles. A one-time chatbox notice explains the rename after login.

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

## Item rules and exceptions

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

## Other mods / sources

This project combines features from, and credits, these plugins. Links identify the source versions reviewed for this release. Adapted code/catalogs and complete licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and distributed with the plugin.

| Source plugin | Features included |
|---|---|
| [No Examine](https://github.com/Skretzo/runelite-plugins/tree/ea506dbbfd2aa70dd024f2fc7b761786e2ced91a) | Category-specific Examine removal, keeping lone Examine, right-click Cancel/Walk here/Set heading cleanup |
| [Not Empty](https://github.com/dekvall/runelite-external-plugins/tree/0c1c5cf2a8588ac3b017e7026c393d2eee4980c3) | Dosed-item Empty protection |
| [No Misclicks In Bank](https://github.com/Welkon1/No-Misclicks-In-Bank/tree/6f76aa102a04525bdce2424b57b4ad899ba82e8b) | Independent Wield, Wear, Drink and Eat bank guards |
| [Item Option Hider](https://github.com/kekingbd/item-option-hider/tree/5375d699a8661c157c7e26b699a9c46401237c7e) | Item action/name rules, wildcards and item-on-item targets |
| [Menu Colorizer](https://github.com/dgalambos/menu-colorizer/tree/3e73b989615aecb2813f0b74bb9ecdf5ba5451ac) | Cosmetic action colors, interactive picker/reset and submenus |
| [Tool Required](https://github.com/Unmoon/tool-required/tree/031e4986c2235f58888cf158666a02f3497cac60) | Axe/pickaxe requirements and tool catalog |
| [Home Improvement](https://github.com/Mike-U5/rl-plugin-home-improvement/tree/6b7cca56d95d794bae252f5f7b58b8d2d5f6027e) | House building-mode cleanup and Mahogany Homes catalog |
| [Misclick Guard](https://github.com/willzero123/misclick-guard/tree/a49e33e7576dbf60ec3caa39be4f470b6be71f47) | Button matching, click-origin protection and Cancel fallback |
| [Ground Loot Icons](https://github.com/Fiffers/ground-loot-icons/tree/8e3e529f27aef3b7a5323ef8fef4f565d8b00be2) | Take-menu item icons and row geometry |
| [Mouseover Options+](https://github.com/Petzep/mouseover-options/tree/872f6bf6a0817e5d6292749c2f3d4ae8a8eb9600) | Additional-option counts, minimum threshold, Examine/widget switches and text color |
| [No Use Player](https://github.com/oohwooh/no-use-players/tree/e76b5612cea2f9fe68b0ba6cecd2bf11b1a5d656) | Item-only player-use protection and default allowed items |
| [Don't Telegrab NPCs](https://github.com/ldavid432/dont-telekinetic-grab-npcs/tree/71b6675364d67a65eb0023667c42863eeb0e3ea7) | Telegrab-specific NPC protection and useful-target catalog |

Users of the features in this table can consolidate them here after checking their settings. This is feature coverage, not a claim that every setting in each source plugin has been reproduced. Cancel/Walk/Set-heading cleanup applies only to opened menus. Option counts use a standard hover tooltip rather than Mouseover Options+'s custom badge layout. Useful Telegrab targets are always allowed rather than individually switchable. Hold-to-reveal is shared rather than Examine-only. Mahogany Homes cleanup preserves non-Remove actions.

Keep RuneLite's Menu Entry Swapper for swaps and specialist plugins for their remaining features. Disable overlapping filters/colors before comparing results: this plugin cannot reveal options another plugin has removed.

Menu Entry Swapper's Shift-right-click customization remains available with Examine hiding and menu colors enabled. Improved Right-Clicks keeps its native Examine lookup entries unchanged while building that menu, then applies the configured cleanup after the swap controls have been added.

## Development

Java 11 is used for the build. Run `gradlew.bat test` for automated checks or `gradlew.bat run` to launch the development client. Tests cover filtering boundaries, order/callback preservation, contexts, profiles/import validation, click origin, tooltip ownership, reveal and cosmetic restoration. No additional production runtime dependency is required.

A short live smoke check is still needed for real menu rendering: try a dosed potion and bank item, one protected button through both click paths, a tree with/without an axe, a house menu outside/inside building mode, a colored submenu, and ground loot icons. Disable/re-enable once while a menu is open. Extensive repeated testing is not required, but automated checks do not verify actual in-game rendering.

Ground loot icons decorate the main right-click menu only. Submenus keep their original target text because RuneLite does not expose whether a submenu is open for reliable icon positioning. Reopening a menu and disabling icons restore the target text owned by Improved Right-Clicks.

Tool guards leave the knife-cut Temple Trekking swamp vines and quest Cave entrance interactions available. Ordinary axe and pickaxe protections remain independent.


### Custom ground-menu icons

Open **Ground loot menu icons** in the side panel, enable **Show icons in Take menus**, and select **Native**, **Custom**, or **Both**. Native shows item pictures; Custom shows imported or manually assigned icons; Both shows custom and item pictures together. Custom leaves items without a visible assigned icon undecorated; it never substitutes a native icon for a missing custom image. Both retains the native item picture when no custom icon is visible. Imported filters honor the final `hidden` and `hideOverlay` values after matching rule overrides. Manually assigned item icons take priority over imported filter rules. Both avoids duplicating Current Item or the same item ID. Menu colors stay separate.

Enable **Both: respect filter visibility** under Presentation to show both icons only when a visible custom icon exists. Hidden, unassigned or unavailable custom icons then suppress both pictures. The option defaults off, does not affect Native mode, and is included in profiles, import/export and undo. Manual assignments remain explicit overrides.

**Loot Filters is not linked automatically.** Click **Import Loot Filters script** and import the same Filterscape export you use there. Confirm the import; its rule count appears in the side panel. Import again when you change that script. The original Loot Filters setup is never modified.

Choose **Manage icons / backups** to assign Heart, Star, Diamond, Current Item or an image file to an exact item name. Use tradeable-item search or enter an untradeable item's exact name. Manual assignments take priority over the imported filter; the editor labels them separately from imported rules.

**Import Loot Filters** accepts a generated Filterscape text file (`.rs2f` or `.txt`) or pasted text. It preserves the original RS2F source and evaluates macros (including parameters), boolean conditions, wildcard names, item IDs, quantity and value comparisons, tradeability, noted/stackable state, ownership, account type, and areas. `apply` inherits icon values; terminal `rule`/`if` stops at the first matching rule, including rules without an explicit icon. These rules choose icons beside **Take** menu entries only. Ground overlays, hiding, sorting, sounds, loot beams and colors are not activated by this import.

`CurrentItem()`, `Item(id)`, `Sprite(id,index)` and `File("name.png")` icons are supported. If a filter references files, select its icons directory (usually `.runelite/loot-filters/icons`). Every referenced image must be available before the import applies. PNG, JPEG and GIF are decoded to a transparent, aspect-preserving 17 x 15 menu image; GIF uses its first frame. Copies are embedded in our configuration, so uninstalling Loot Filters or removing the original files does not break the imported setup. Invalid paths, missing files and malformed filters leave the previous setup intact. Limits: 1 MB filter text, 128 images, 2 MB / 2048 x 2048 per source image, bounded macro expansion and rule nesting.

Review the rule/image counts before applying. A new import replaces the previous imported filter; manual assignments stay. **Undo filter import** swaps with the previous saved filter. **Filter setup / backup** exports/imports a self-contained JSON bundle with compressed original source and embedded images, or disables the imported filter. **Import/export icons** and **Undo / redo** separately manage manual assignments. Data is saved per RuneLite profile in our `improvedmenus` configuration. Imports never modify Loot Filters files or settings and never fetch a filter or image from the network.

Menu entries do not always identify which of several identical ground stacks they refer to. If those stacks would produce different visible icons, we omit the custom icon rather than guess; Both still shows the native item picture. Submenu icon drawing remains unsupported by the existing public menu-bounds integration.

The RS2F lexer, preprocessor, parser, condition classes and preamble are adapted from [riktenx/loot-filters at 993fd054](https://github.com/riktenx/loot-filters/tree/993fd054e08d42a77d3415acbb9ac7c2797726ed), under its [BSD 2-Clause license](src/main/resources/com/improvedmenus/loot-filters-LICENSE.txt). Our adapter supplies read-only ground-item facts and consumes only the chosen icon; visibility properties select whether an icon is shown; other display/audio properties never change ground rendering, menu entries or colors. No changes or submissions are made to the upstream plugin. The supplied Filterscape, Joe and Ironman presets are used as local compatibility fixtures, not bundled in the plugin.

### Optional Loot Filters compatibility (pending upstream support)

Enable **Loot Filters compatibility** directly above **Icon source**, then choose **Custom** or **Both**. A supporting Loot Filters version supplies its selected icons, including custom images, to Take menus automatically. Native uses ordinary item pictures. Colors remain controlled separately. No script or image upload is required for the connection.

Until a supporting Loot Filters version is released, the connection supplies no custom icon. Custom leaves the row undecorated; Both retains its native icon. Existing saved assignments and imported backups remain intact; turn compatibility off to use the existing imported rules. Manual item assignments continue to take priority. The side panel reports whether Loot Filters responded after opening a Take menu.

**Use Loot Filters in menu** is a separate, optional setting under Presentation. It uses the provider's resolved ordering while retaining the menu's existing hidden-item priority for Take rows in opened menus. It keeps every action available and leaves non-loot rows in their existing positions. It does not change normal left-click behavior, saved menu rules, or icon selection. Hidden ground overlays are not treated as instructions to delete Take actions.

**Use Loot Filters menu colors**, under Colors, additionally applies the provider's resolved item-name color when **Use Loot Filters in menu** is on. Hidden-row colors stay as Loot Filters sets them. Your action-color rules stay separate. Both new switches default off. They are included in settings profiles, import/export and undo.

| Settings | Result |
| --- | --- |
| Menu integration off | Existing menu behavior and colors; icon settings still work independently |
| Menu integration on, menu colors off | Loot Filters Take-row ordering/priority; existing colors |
| Menu integration on, menu colors on | Loot Filters Take-row ordering/priority and item-name colors |
| Icon source Native | Native item pictures, regardless of the menu switches |
| Icon source Custom / Both with compatibility on | Resolved Loot Filters icons; existing manual overrides and Both visibility option remain in effect |
| Provider unavailable or conflicting stacks | No extra menu changes; existing Custom/Both fallback behavior |

The connection sends `resolved-display-request-v1` with item coordinates and reads the resolved display from the same message after dispatch. It requests already evaluated settings when opening a menu, including cached custom images; it does not read another plugin's files or re-evaluate its scripts. No sounds or ground overlays are created by this integration. Loot Filters' own menu changes continue to be controlled by that plugin.

### Additional option indicator

Under Presentation, **Hide player option counts** excludes player actions while retaining loot and object actions underneath them. **Hide counts when Walk here** suppresses deprioritized NPC/object counts when walking is preferred; ground items and widgets remain eligible. These switches affect only the hover indicator, not the menu entries. Both start off.

**Option count anchor** offers Below left (the existing default), Below right, Above left and Above right relative to the tooltip stack. RuneLite still controls the stack position relative to the cursor. All options are saved in profiles and support import/export and undo.
