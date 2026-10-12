# Ground Item Entity Hider

Hide selected ground item models by item name while leaving their menu actions available.

In the plugin configuration, enter a comma-separated list of item names to hide. Matching uses RuneLite wildcard patterns, with `*` matching any sequence of characters. For example:

```text
Monkey corpse, Rune*, *platebody
```

This hides items whose names exactly match `Monkey corpse`, start with `Rune`, or end with `platebody`. Leave the setting empty to show all ground items. Hidden items remain available through their usual menu actions. The plugin does not send any game actions or make network requests.