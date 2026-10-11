package com.improvedmenus;

import java.util.*;

/** Explicit editable keys. Profiles cannot set unrelated RuneLite configuration. */
final class SettingsCatalog
{
    static final class Setting
    {
        final String key, label, section, defaultValue;
        final String[] choices;
        Setting(String key, String label, String section, String defaultValue, String... choices)
        { this.key=key; this.label=label; this.section=section; this.defaultValue=defaultValue; this.choices=choices; }
        void validate(String value)
        {
            if (value == null) { throw new IllegalArgumentException("Missing value for " + label); }
            if (choices.length != 0 && !Arrays.asList(choices).contains(value)) { throw new IllegalArgumentException("Invalid value for " + label); }
            if (key.equals("itemRules")) { MenuRules.parseItems(value); }
            else if (key.equals("colors")) { MenuRules.parseColors(value); }
            else if (key.equals("exceptions")) { MenuRules.parseExceptions(value); }
            else if (choices.length == 0) { MenuRules.bounded(value); }
        }
    }
    static final Map<String, Setting> ALL = new LinkedHashMap<>();
    static
    {
        add("revealKey", "Hold to reveal", "General", "CONTROL", "CONTROL", "SHIFT", "ALT", "NONE");
        bool("potionEmpty", "No Empty on potions", "Inventory", false);
        bool("examineInventory", "Hide item Examine", "Examine", false); bool("examineGround", "Hide ground Examine", "Examine", false);
        bool("examineNpc", "Hide NPC Examine", "Examine", false); bool("examineObject", "Hide scenery Examine", "Examine", false);
        bool("keepLoneNpc", "Keep lone NPC Examine", "Examine", true); bool("keepLoneObject", "Keep lone scenery Examine", "Examine", true);
        add("exceptions", "Always keep names (one per line)", "Rules", "");
        add("itemRules", "Item rules: action,item name", "Rules", "");
        add("colors", "Colors: target:action:#RRGGBB;", "Colors", "");
        bool("enableColors", "Enable colors", "Colors", true); bool("colorPicker", "Shift menu color editor", "Colors", true);
        bool("chop", "Require axe", "Tools", false); bool("mine", "Require pickaxe", "Tools", false);
        bool("house", "House menu cleanup", "House", false); bool("mahogany", "Mahogany Homes cleanup", "House", false);
        add("autoRetaliate", "Auto Retaliate", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("xpDrops", "XP Drops", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("alwaysSetPlaceholders", "Bank placeholders", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("report", "Report", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        bool("deprioritizeLeftClickOffEntries", "Deprioritize protected buttons", "Buttons", true);
        bool("groundIcons", "Ground loot icons", "Presentation", false);
        bool("optionCount", "Additional option count", "Presentation", false);
        bool("hideCancel", "Hide Cancel (right-click menu)", "Menu cleanup", false);
        bool("hideWalkHere", "Hide Walk here (right-click menu)", "Menu cleanup", false);
        bool("noUsePlayer", "No Use on Player", "Target protection", false);
        add("playerUseItems", "Allow items on players (comma separated)", "Target protection", TargetProtections.DEFAULT_PLAYER_ITEMS);
        bool("noTelegrabNpc", "Don't Telegrab NPCs", "Target protection", false);
        add("telegrabNpcNames", "Allow Telegrab NPC names (comma separated)", "Target protection", "");
        bool("bankWield", "Hide Wield", "Bank", false); bool("bankWear", "Hide Wear", "Bank", false);
        bool("bankDrink", "Hide Drink", "Bank", false); bool("bankEat", "Hide Eat", "Bank", false);
    }
    private static void bool(String key, String label, String section, boolean value) { add(key,label,section,Boolean.toString(value),"false","true"); }
    private static void add(String key, String label, String section, String value, String... choices) { ALL.put(key,new Setting(key,label,section,value,choices)); }
    private SettingsCatalog() {}
}
