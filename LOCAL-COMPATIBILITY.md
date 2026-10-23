# Local Loot Filters compatibility

Improved Right-Clicks reads the selected filter from regular Loot Filters. It evaluates the saved RS2F rules inside Improved Right-Clicks and loads icons using RuneLite services and its own file-image cache. It does not need the proposed PluginMessage API or a modified Loot Filters build.

- Enable regular Loot Filters and Improved Right-Clicks.
- In plugin settings, turn on Ground loot icons and Loot Filters compatibility.
- Choose Custom or Both. Native remains the unchanged default.
- Use Loot Filters in menu and its color switch remain separate and optional.
- Existing manual assignments still take priority. Imported backups stay saved and remain usable with compatibility off.

The source selection and relevant settings are read from the current RuneLite profile. Highlighted/hidden lists take precedence over the script; compact mode selects the current item. Rule ordering, apply/terminal rules, account type, ownership, quantity, value and region conditions are evaluated locally. Different same-ID stacks that resolve differently do not get an arbitrary shared icon. Non-loot menu actions stay intact.

Custom files are read from .runelite/loot-filters/filters and icons from .runelite/loot-filters/icons. Reads and compilation run off the client thread. Saved filters are checked about every two seconds; image files are retried/refreshed on demand after five seconds. Profile changes, configuration changes and disabling either integration/plugin invalidate previous results. Pending file images can appear in an already open menu.

Limits: this reads saved files and settings, not Loot Filters' in-memory program. If editing a script externally, reload it in Loot Filters too. Temporary overlay hiding with Alt/double-tap, camera occlusion and distance are presentation states, not filter icon visibility; they do not suppress menu icons. Three bundled defaults match the regular Hub version inspected, and need maintenance when upstream changes them. Scripts over 1 MB or excessive parser complexity, images over 2 MiB, and paths outside the designated directories are rejected visibly. Invalid/missing selected filters do not keep using the last filter.

For Hub review: automatic access to the two fixed Loot Filters directories uses Filepath.Unchecked. Access is read-only, bounded and confined to selected filter files and referenced images. This does not migrate the other plugin's directory, write its configuration, start it automatically, or fetch files from the network.
