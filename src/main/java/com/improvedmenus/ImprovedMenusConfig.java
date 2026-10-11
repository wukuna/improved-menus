package com.improvedmenus;

import net.runelite.client.config.*;

@ConfigGroup(ImprovedMenusConfig.GROUP)
public interface ImprovedMenusConfig extends Config
{
    String GROUP = "improvedmenus";
    enum RevealKey { CONTROL, SHIFT, ALT, NONE }
    @ConfigItem(keyName="revealKey", name="Hold to reveal", description="Hold before opening a menu to reveal options hidden by Menu Workshop", position=0)
    default RevealKey revealKey() { return RevealKey.CONTROL; }
    @ConfigItem(keyName="potionEmpty", name="No Empty on potions", description="Hide Empty on dosed inventory potions", position=1)
    default boolean potionEmpty() { return false; }
    @ConfigItem(keyName="bankWield", name="Bank: hide Wield", description="Hide inventory Wield while the bank is open", position=35)
    default boolean bankWield() { return false; }
    @ConfigItem(keyName="bankWear", name="Bank: hide Wear", description="Hide inventory Wear while the bank is open", position=36)
    default boolean bankWear() { return false; }
    @ConfigItem(keyName="bankDrink", name="Bank: hide Drink", description="Hide inventory Drink while the bank is open", position=37)
    default boolean bankDrink() { return false; }
    @ConfigItem(keyName="bankEat", name="Bank: hide Eat", description="Hide inventory Eat while the bank is open", position=38)
    default boolean bankEat() { return false; }
    @ConfigItem(keyName="examineInventory", name="Hide inventory Examine", description="Remove item Examine entries", position=6)
    default boolean examineInventory() { return false; }
    @ConfigItem(keyName="examineGround", name="Hide ground Examine", description="Remove ground item Examine entries", position=7)
    default boolean examineGround() { return false; }
    @ConfigItem(keyName="examineNpc", name="Hide NPC Examine", description="Remove NPC Examine entries", position=8)
    default boolean examineNpc() { return false; }
    @ConfigItem(keyName="examineObject", name="Hide object Examine", description="Remove scenery Examine entries", position=9)
    default boolean examineObject() { return false; }
    @ConfigItem(keyName="keepLoneNpc", name="Keep lone NPC Examine", description="Retain Examine when the NPC has no other actions", position=10)
    default boolean keepLoneNpc() { return true; }
    @ConfigItem(keyName="keepLoneObject", name="Keep lone object Examine", description="Retain Examine when scenery has no other actions", position=11)
    default boolean keepLoneObject() { return true; }
    @ConfigItem(keyName="exceptions", name="Always keep names", description="One name per line; * wildcard. Optional [inventory], [bank] or [ground] prefix", position=12)
    default String exceptions() { return ""; }
    @ConfigItem(keyName="itemRules", name="Item action rules", description="One action,item name per line. * wildcard. # comments. Optional [inventory], [bank], [ground] prefix. Items only", position=13)
    default String itemRules() { return ""; }
    @ConfigItem(keyName="colors", name="Menu colors", description="target:action:#RRGGBB separated by semicolons. * wildcard. Cosmetic only", position=14)
    default String colors() { return ""; }
    @ConfigItem(keyName="enableColors", name="Enable colors", description="Color supported menu options, including submenus", position=15)
    default boolean enableColors() { return true; }
    @ConfigItem(keyName="colorPicker", name="Shift menu color editor", description="Hold Shift and open a menu to add local color controls", position=16)
    default boolean colorPicker() { return true; }
    @ConfigItem(keyName="chop", name="Require axe", description="Hide tree Chop/Cut when no axe is carried or equipped", position=17)
    default boolean chop() { return false; }
    @ConfigItem(keyName="mine", name="Require pickaxe", description="Hide Mine when no pickaxe is carried or equipped", position=18)
    default boolean mine() { return false; }
    @ConfigItem(keyName="house", name="House menu cleanup", description="Hide furniture building actions outside building mode in your house", position=19)
    default boolean house() { return false; }
    @ConfigItem(keyName="mahogany", name="Mahogany Homes cleanup", description="Hide Remove on furniture that does not need replacement", position=20)
    default boolean mahogany() { return false; }
    @ConfigItem(keyName="autoRetaliate", name="Auto Retaliate protection", description="Normal, right-click only or hidden", position=21)
    default ClickMode autoRetaliate() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="xpDrops", name="XP Drops protection", description="Normal, right-click only or hidden", position=22)
    default ClickMode xpDrops() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="alwaysSetPlaceholders", name="Bank placeholders protection", description="Normal, right-click only or hidden", position=23)
    default ClickMode alwaysSetPlaceholders() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="report", name="Report protection", description="Normal, right-click only or hidden", position=24)
    default ClickMode report() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="deprioritizeLeftClickOffEntries", name="Deprioritize protected buttons", description="Make another action or Cancel the default", position=25)
    default boolean deprioritizeLeftClickOffEntries() { return true; }
    @ConfigItem(keyName="groundIcons", name="Ground loot icons", description="Show item icons in Take menus", position=26)
    default boolean groundIcons() { return false; }
    @ConfigItem(keyName="optionCount", name="Additional option count", description="Show the number of other available actions on hover", position=27)
    default boolean optionCount() { return false; }
    @ConfigItem(keyName="hideCancel", name="Hide Cancel", description="Hide Cancel in opened right-click menus; keep a safe fallback if nothing remains", position=28)
    default boolean hideCancel() { return false; }
    @ConfigItem(keyName="hideWalkHere", name="Hide Walk here", description="Hide Walk here in opened right-click menus; normal left-click walking stays available", position=29)
    default boolean hideWalkHere() { return false; }
    @ConfigItem(keyName="noUsePlayer", name="No Use on Player", description="Hide item Use on players; preserve spells and allowlisted items", position=30)
    default boolean noUsePlayer() { return false; }
    @ConfigItem(keyName="playerUseItems", name="Allow items on players", description="Allowed item names, separated by commas", position=31)
    default String playerUseItems() { return TargetProtections.DEFAULT_PLAYER_ITEMS; }
    @ConfigItem(keyName="noTelegrabNpc", name="Don't Telegrab NPCs", description="Hide Telekinetic Grab on NPCs, preserving known quest, training and Sailing targets", position=32)
    default boolean noTelegrabNpc() { return false; }
    @ConfigItem(keyName="telegrabNpcNames", name="Allow Telegrab NPC names", description="Additional allowed NPC names, separated by commas", position=33)
    default String telegrabNpcNames() { return ""; }
    @ConfigItem(keyName="hideSetHeading", name="Hide Set heading", description="Hide Set heading in opened right-click menus", position=34)
    default boolean hideSetHeading() { return false; }
    @ConfigItem(keyName="minimumOptions", name="Minimum extra options", description="Only show the option count at or above this number", position=39)
    @Range(min=1,max=500)
    default int minimumOptions() { return 1; }
    @ConfigItem(keyName="countExamine", name="Count Examine", description="Include Examine in the additional-option count", position=40)
    default boolean countExamine() { return true; }
    @ConfigItem(keyName="countOnWidgets", name="Show count on interface widgets", description="Show counts on inventory, bank, shop and other interface widgets", position=41)
    default boolean countOnWidgets() { return true; }
    @ConfigItem(keyName="optionCountColor", name="Option count text color", description="Color in #RRGGBB format", position=42)
    default String optionCountColor() { return "#ffffff"; }
    @ConfigItem(keyName="defaultPickerColor", name="Default picker color", description="Initial color for entries with no matching color rule, in #RRGGBB format", position=43)
    default String defaultPickerColor() { return "#00ff00"; }
}
