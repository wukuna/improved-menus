package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;

/** Optional message-based integration; no dependency on Loot Filters internals or files. */
@Singleton
class LootFiltersCompatibility {
    @Inject private Client client;
    @Inject private EventBus events;
    private String pending;
    private Result result;
    private boolean collecting;
    private final Map<Tile, Map<Integer, Result>> menuResults = new IdentityHashMap<>();
    private volatile String status = "Open a Take menu to check Loot Filters compatibility.";
    static final class Result {
        final BufferedImage image;
        final boolean itemIcon;
        final boolean hidden, hideOverlay, deprioritized;
        final int menuSort;
        final Color menuColor;
        Result(BufferedImage image, boolean itemIcon) { this(image,itemIcon,false,false,0,false,null); }
        Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int menuSort, boolean deprioritized, Color menuColor) {
            this.image=image; this.itemIcon=itemIcon; this.hidden=hidden; this.hideOverlay=hideOverlay;
            this.menuSort=menuSort; this.deprioritized=deprioritized; this.menuColor=menuColor;
        }
    }
    String status() { return status; }
    void reset() { pending=null; result=null; endMenu(); status="Open a Take menu to check Loot Filters compatibility."; }
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
        if(!collecting) return request(tile,entry.getIdentifier());
        Map<Integer,Result> tileResults=menuResults.computeIfAbsent(tile,ignored -> new HashMap<>());
        int id=entry.getIdentifier();
        if(!tileResults.containsKey(id)) tileResults.put(id,request(tile,id));
        return tileResults.get(id);
    }
    Result request(Tile tile, int itemId) {
        if(!client.isClientThread()) return null;
        pending=UUID.randomUUID().toString(); result=null;
        status="Compatible Loot Filters not responding; no display settings available.";
        try {
            events.post(new PluginMessage("loot-filters","resolved-display-request-v1",
                Map.of("requestId",pending,"tile",tile,"itemId",itemId)));
            return result;
        } finally { pending=null; result=null; }
    }
    @Subscribe public void onPluginMessage(PluginMessage message) {
        if(!client.isClientThread() || pending==null || !"loot-filters".equals(message.getNamespace())
            || !"resolved-display-response-v1".equals(message.getName())
            || !pending.equals(message.getData().get("requestId"))) return;
        Map<String,Object> data=message.getData();
        result=decode(data);
        if(result!=null) {
            status="Connected to Loot Filters. Icon source, menu behavior and colors use their separate settings.";
        } else {
            status="Loot Filters responded; this item has no ready, unambiguous display settings.";
        }
    }
    static Result decode(Map<String,Object> data) {
        if(!"ok".equals(data.get("status")) || !(data.get("display") instanceof Map) || !(data.get("menu") instanceof Map)) return null;
        Map<?,?> display=(Map<?,?>)data.get("display"), menu=(Map<?,?>)data.get("menu");
        if(!(display.get("hidden") instanceof Boolean) || !(display.get("hideOverlay") instanceof Boolean)
            || !(menu.get("sort") instanceof Integer) || !(menu.get("deprioritized") instanceof Boolean)
            || !(menu.get("textColor") instanceof Color) || !(data.get("itemIcon") instanceof Boolean)) return null;
        Object iconStatus=data.get("iconStatus");
        if(!"ready".equals(iconStatus) && !"hidden".equals(iconStatus) && !"none".equals(iconStatus) && !"not-ready".equals(iconStatus)) return null;
        if("ready".equals(iconStatus) && !(data.get("icon") instanceof BufferedImage)) return null;
        boolean hidden=(Boolean)display.get("hidden"), hideOverlay=(Boolean)display.get("hideOverlay");
        BufferedImage image=!hidden && !hideOverlay && "ready".equals(iconStatus) ? (BufferedImage)data.get("icon") : null;
        return new Result(image,(Boolean)data.get("itemIcon"),hidden,hideOverlay,(Integer)menu.get("sort"),(Boolean)menu.get("deprioritized"),(Color)menu.get("textColor"));
    }
}
