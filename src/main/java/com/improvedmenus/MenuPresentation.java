package com.improvedmenus;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Icon row geometry adapted from Fiffers/ground-loot-icons; see bundled license. */
@Singleton
final class MenuPresentation extends Overlay
{
    private static final Set<MenuAction> GROUND = EnumSet.of(MenuAction.GROUND_ITEM_FIRST_OPTION,MenuAction.GROUND_ITEM_SECOND_OPTION,
        MenuAction.GROUND_ITEM_THIRD_OPTION,MenuAction.GROUND_ITEM_FOURTH_OPTION,MenuAction.GROUND_ITEM_FIFTH_OPTION);
    @Inject private Client client;
    @Inject private ImprovedMenusConfig config;
    @Inject private ItemManager items;
    @Inject private ChatIconManager icons;
    @Inject private TooltipManager tooltips;
    private int placeholder = -1;
    private final IdentityHashMap<MenuEntry,TargetEdit> ownedTargets = new IdentityHashMap<>();
    private String marker = "";

    MenuPresentation() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP); }
    void start()
    {
        // ChatIconManager has no removal API. Reuse this registration on re-enable.
        if (placeholder == -1) { placeholder=icons.registerChatIcon(new BufferedImage(17,15,BufferedImage.TYPE_INT_ARGB)); }
    }
    void stop()
    {
        // Restore a decorated open menu; do not modify entries remapped since it was built.
        ownedTargets.forEach((entry,edit) -> { if (edit.matches(entry)) { entry.setTarget(edit.original); } });
        ownedTargets.clear();
    }
    void decorate(Menu menu)
    {
        stop();
        if (!config.groundIcons()) { return; }
        int index=icons.chatIconIndex(placeholder); if (index == -1) { return; }
        marker="<img=" + index + ">";
        // Submenu bounds are not valid unless open, and the API does not expose
        // that state. Keep submenu text native rather than reserving blank icons.
        if (menu == null) { return; }
        for (MenuEntry entry : menu.getMenuEntries())
        {
            if (GROUND.contains(entry.getType()) && "Take".equals(MenuRules.plain(entry.getOption())))
            {
                String original=entry.getTarget();
                String decorated = marker + original;
                ownedTargets.put(entry,new TargetEdit(entry,original,decorated));
                entry.setTarget(decorated);
            }
        }
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        if (client.isMenuOpen())
        {
            if (config.groundIcons()) { draw(graphics,client.getMenu()); }
        }
        else if (config.optionCount())
        {
            int count=additionalCount(client.getMenu().getMenuEntries(),config.countExamine(),config.countOnWidgets());
            if (count >= Math.max(1,config.minimumOptions()) && client.getMouseCanvasPosition().getX() >= 0)
            {
                java.awt.Color color=SettingsCatalog.colorOrDefault(config.optionCountColor(),java.awt.Color.WHITE);
                tooltips.add(new Tooltip(String.format("<col=%06x>+%d options</col>",color.getRGB() & 0xffffff,count)));
            }
        }
        return null;
    }
    static int additionalCount(MenuEntry[] entries)
    { return additionalCount(entries,true,true); }
    static int additionalCount(MenuEntry[] entries,boolean countExamine,boolean countOnWidgets)
    {
        int count=0;
        for (MenuEntry entry : entries)
        {
            MenuAction type=entry.getType();
            if (!countOnWidgets && entry.getWidget() != null) { continue; }
            if (!countExamine && (type == MenuAction.EXAMINE_NPC || type == MenuAction.EXAMINE_OBJECT
                || type == MenuAction.EXAMINE_ITEM_GROUND || MenuRules.plain(entry.getOption()).equalsIgnoreCase("Examine"))) { continue; }
            if (type != MenuAction.CANCEL && type != MenuAction.WALK && type != MenuAction.WIDGET_CONTINUE
                && type != MenuAction.RUNELITE && type != MenuAction.RUNELITE_OVERLAY && type != MenuAction.RUNELITE_OVERLAY_CONFIG)
            { count++; }
        }
        return Math.max(0,count-1);
    }
    private void draw(Graphics2D g,Menu menu)
    {
        if (menu == null || menu.getMenuWidth() <= 0 || menu.getMenuHeight() <= 0) { return; }
        MenuEntry[] entries=menu.getMenuEntries();
        int scroll=client.isMenuScrollable() ? client.getMenuScroll() : 0;
        FontMetrics metrics=g.getFontMetrics(FontManager.getRunescapeFont());
        for (int i=0;i<entries.length;i++)
        {
            MenuEntry entry=entries[i];
            if (!GROUND.contains(entry.getType()) || !"Take".equals(MenuRules.plain(entry.getOption())) || !entry.getTarget().startsWith(marker)) { continue; }
            int y=menu.getMenuY()+19+(entries.length-1-i-scroll)*15;
            if (y < menu.getMenuY()+19 || y+15 > menu.getMenuY()+menu.getMenuHeight()) { continue; }
            int x=menu.getMenuX()+8+metrics.stringWidth(MenuRules.plain(entry.getOption())+" ");
            g.drawImage(items.getImage(entry.getIdentifier(),Integer.MAX_VALUE,false),x,y,17,15,null);
        }
    }
    private static final class TargetEdit
    {
        final String original, decorated;
        final MenuAction type;
        final int identifier, param0, param1;
        TargetEdit(MenuEntry entry,String original,String decorated)
        {
            this.original=original; this.decorated=decorated; type=entry.getType();
            identifier=entry.getIdentifier(); param0=entry.getParam0(); param1=entry.getParam1();
        }
        boolean matches(MenuEntry entry)
        {
            return entry.getTarget().equals(decorated) && entry.getType()==type
                && entry.getIdentifier()==identifier && entry.getParam0()==param0 && entry.getParam1()==param1;
        }
    }
}
