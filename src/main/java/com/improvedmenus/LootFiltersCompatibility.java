package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

/** Optional message-based integration; no dependency on Loot Filters internals or files. */
@Singleton
class LootFiltersCompatibility {
    @Inject private Client client;
    @Inject private EventBus events;
    private boolean collecting;
    private final Map<Tile, Map<Integer, Result>> menuResults = new IdentityHashMap<>();
    private volatile String status = "Open a Take menu to check Loot Filters compatibility.";
    static final class Result {
        final BufferedImage image;
        final boolean itemIcon;
        final boolean hidden, hideOverlay;
        final int menuSort;
        final Color menuColor;
        Result(BufferedImage image, boolean itemIcon) { this(image,itemIcon,false,false,0,null); }
        Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int menuSort, Color menuColor) {
            this.image=image; this.itemIcon=itemIcon; this.hidden=hidden; this.hideOverlay=hideOverlay;
            this.menuSort=menuSort; this.menuColor=menuColor;
        }
    }
    String status() { return status; }
    void reset() { endMenu(); status="Open a Take menu to check Loot Filters compatibility."; }
    void beginMenu() { menuResults.clear(); collecting=true; }
    void endMenu() { menuResults.clear(); collecting=false; }
    Result find(MenuEntry entry) {
        if (!client.isClientThread()) return null;
        WorldView view=client.getWorldView(entry.getWorldViewId());
        if(view==null || view.getScene()==null) return null;
        Tile[][][] tiles=view.getScene().getTiles();
        int plane=view.getPlane(), x=entry.getParam0(), y=entry.getParam1();
        if(tiles==null || plane<0 || plane>=tiles.length || tiles[plane]==null
            || x<0 || x>=tiles[plane].length || tiles[plane][x]==null
            || y<0 || y>=tiles[plane][x].length || tiles[plane][x][y]==null) return null;
        Tile tile=tiles[plane][x][y];
        if(!collecting) return request(view,x,y,plane,entry.getIdentifier());
        Map<Integer,Result> tileResults=menuResults.computeIfAbsent(tile,ignored -> new HashMap<>());
        int id=entry.getIdentifier();
        if(!tileResults.containsKey(id)) tileResults.put(id,request(view,x,y,plane,id));
        return tileResults.get(id);
    }
    Result request(WorldView view, int sceneX, int sceneY, int plane, int itemId) {
        if(!client.isClientThread()) return null;
        Map<String,Object> data=new HashMap<>();
        data.put("worldViewId",view.getId()); data.put("x",view.getBaseX()+sceneX);
        data.put("y",view.getBaseY()+sceneY); data.put("z",plane); data.put("itemId",itemId);
        events.post(new PluginMessage("loot-filters","resolved-display-request-v1",data));
        Result result=decode(data,itemId);
        if(result!=null) {
            status="Connected to Loot Filters. Icon source, menu behavior and colors use their separate settings.";
        } else if(data.containsKey("status")) {
            status="Loot Filters responded; this item has no ready, unambiguous display settings.";
        } else {
            status="Compatible Loot Filters not responding; no display settings available.";
        }
        return result;
    }
    static Result decode(Map<String,Object> data,int itemId) {
        if(!"ok".equals(data.get("status")) || !(data.get("display") instanceof Map)) return null;
        Map<?,?> display=(Map<?,?>)data.get("display");
        if(!(display.get("hidden") instanceof Boolean) || !(display.get("hideOverlay") instanceof Boolean)
            || !(display.get("menuSort") instanceof Integer) || !(display.get("menuTextColor") instanceof Color)) return null;
        Map<?,?> icon=null;
        if(display.get("icon")!=null) {
            if(!(display.get("icon") instanceof Map)) return null;
            icon=(Map<?,?>)display.get("icon");
            if(!(icon.get("type") instanceof String) || (icon.get("image")!=null && !(icon.get("image") instanceof BufferedImage))) return null;
        }
        boolean hidden=(Boolean)display.get("hidden"), hideOverlay=(Boolean)display.get("hideOverlay");
        BufferedImage image=!hidden && !hideOverlay && icon!=null ? (BufferedImage)icon.get("image") : null;
        boolean sameItem=icon!=null && ("item".equals(icon.get("type")) || "current-item".equals(icon.get("type")))
            && Integer.valueOf(itemId).equals(icon.get("id"));
        return new Result(image,sameItem,hidden,hideOverlay,(Integer)display.get("menuSort"),(Color)display.get("menuTextColor"));
    }
}
