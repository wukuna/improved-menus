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
    private final IdentityHashMap<MenuEntry,String> ownedTargets = new IdentityHashMap<>();
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
        ownedTargets.forEach((entry,original) -> { if (entry.getTarget().equals(marker + original)) { entry.setTarget(original); } });
        ownedTargets.clear();
    }
    void decorate(Menu menu)
    {
        ownedTargets.clear();
        if (!config.groundIcons()) { return; }
        int index=icons.chatIconIndex(placeholder); if (index == -1) { return; }
        marker="<img=" + index + ">";
        decorate(menu,0);
    }
    private void decorate(Menu menu,int depth)
    {
        if (depth > 8) { return; }
        for (MenuEntry entry : menu.getMenuEntries())
        {
            if (entry.getSubMenu() != null) { decorate(entry.getSubMenu(),depth+1); }
            if (GROUND.contains(entry.getType()) && "Take".equals(MenuRules.plain(entry.getOption())))
            {
                String original=entry.getTarget();
                if (!original.startsWith(marker)) { ownedTargets.put(entry,original); entry.setTarget(marker+original); }
            }
        }
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        if (client.isMenuOpen())
        {
            if (config.groundIcons()) { draw(graphics,client.getMenu(),0,true); }
        }
        else if (config.optionCount())
        {
            int count=additionalCount(client.getMenu().getMenuEntries());
            if (count > 0 && client.getMouseCanvasPosition().getX() >= 0) { tooltips.add(new Tooltip("+" + count + " options")); }
        }
        return null;
    }
    static int additionalCount(MenuEntry[] entries)
    {
        int count=0;
        for (MenuEntry entry : entries)
        {
            MenuAction type=entry.getType();
            if (type != MenuAction.CANCEL && type != MenuAction.WALK && type != MenuAction.WIDGET_CONTINUE
                && type != MenuAction.RUNELITE && type != MenuAction.RUNELITE_OVERLAY && type != MenuAction.RUNELITE_OVERLAY_CONFIG)
            { count++; }
        }
        return Math.max(0,count-1);
    }
    private void draw(Graphics2D g,Menu menu,int depth,boolean root)
    {
        if (depth > 8 || menu.getMenuWidth() <= 0 || menu.getMenuHeight() <= 0) { return; }
        MenuEntry[] entries=menu.getMenuEntries();
        int scroll=root && client.isMenuScrollable() ? client.getMenuScroll() : 0;
        FontMetrics metrics=g.getFontMetrics(FontManager.getRunescapeFont());
        for (int i=0;i<entries.length;i++)
        {
            MenuEntry entry=entries[i];
            if (entry.getSubMenu() != null) { draw(g,entry.getSubMenu(),depth+1,false); }
            if (!GROUND.contains(entry.getType()) || !"Take".equals(MenuRules.plain(entry.getOption())) || !entry.getTarget().startsWith(marker)) { continue; }
            int y=menu.getMenuY()+19+(entries.length-1-i-scroll)*15;
            if (y < menu.getMenuY()+19 || y+15 > menu.getMenuY()+menu.getMenuHeight()) { continue; }
            int x=menu.getMenuX()+8+metrics.stringWidth(MenuRules.plain(entry.getOption())+" ");
            g.drawImage(items.getImage(entry.getIdentifier(),Integer.MAX_VALUE,false),x,y,17,15,null);
        }
    }
}
