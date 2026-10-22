package com.improvedmenus;

import java.awt.image.BufferedImage;
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
    private volatile String status = "Open a Take menu to check Loot Filters compatibility.";
    static final class Result {
        final BufferedImage image;
        final boolean itemIcon;
        Result(BufferedImage image, boolean itemIcon) { this.image=image; this.itemIcon=itemIcon; }
    }
    String status() { return status; }
    void reset() { pending=null; result=null; status="Open a Take menu to check Loot Filters compatibility."; }
    Result find(MenuEntry entry) {
        if (!client.isClientThread()) return null;
        WorldView view=client.getWorldView(entry.getWorldViewId());
        if(view==null || view.getScene()==null) return null;
        Tile[][][] tiles=view.getScene().getTiles();
        int plane=view.getPlane(), x=entry.getParam0(), y=entry.getParam1();
        if(tiles==null || plane<0 || plane>=tiles.length || tiles[plane]==null
            || x<0 || x>=tiles[plane].length || tiles[plane][x]==null
            || y<0 || y>=tiles[plane][x].length || tiles[plane][x][y]==null) return null;
        return request(tiles[plane][x][y],entry.getIdentifier());
    }
    Result request(Tile tile, int itemId) {
        pending=UUID.randomUUID().toString(); result=null;
        status="Compatible Loot Filters not responding; using native icons.";
        try {
            events.post(new PluginMessage("loot-filters","menu-icon-request-v1",
                Map.of("requestId",pending,"tile",tile,"itemId",itemId)));
            return result;
        } finally { pending=null; result=null; }
    }
    @Subscribe public void onPluginMessage(PluginMessage message) {
        if(pending==null || !"loot-filters".equals(message.getNamespace())
            || !"menu-icon-response-v1".equals(message.getName())
            || !pending.equals(message.getData().get("requestId"))) return;
        Map<String,Object> data=message.getData();
        if("ok".equals(data.get("status")) && data.get("icon") instanceof BufferedImage) {
            result=new Result((BufferedImage)data.get("icon"),Boolean.TRUE.equals(data.get("itemIcon")));
            status="Connected to Loot Filters. Its selected icons appear in Take menus.";
        } else {
            status="Connected to Loot Filters; no unambiguous icon for this item. Using native.";
        }
    }
}
