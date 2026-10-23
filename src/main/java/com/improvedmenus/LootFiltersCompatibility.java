package com.improvedmenus;

import com.improvedmenus.rs2f.LootFilter;
import com.improvedmenus.rs2f.model.BufferedImageProvider;
import com.improvedmenus.rs2f.model.ItemFacts;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Independent, read-only consumer of the regular plugin's selected script and settings. */
@Singleton
class LootFiltersCompatibility
{
    final Client client;
    final ConfigManager configs;
    final ImprovedMenusConfig config;
    final PluginManager plugins;
    final ItemManager items;
    final SpriteManager sprites;
    final LootFilterFileIcons fileIcons;
    final LocalLootFilterSource source;
    @Inject LootFiltersCompatibility(Client client, ConfigManager configs, ImprovedMenusConfig config,
        PluginManager plugins, ItemManager items, SpriteManager sprites, LootFilterFileIcons fileIcons, LocalLootFilterSource source)
    {
        this.client=client; this.configs=configs; this.config=config; this.plugins=plugins;
        this.items=items; this.sprites=sprites; this.fileIcons=fileIcons; this.source=source;
    }
    private boolean collecting;
    private final Map<Tile, Map<Integer, Result>> menuResults = new IdentityHashMap<>();
    private volatile String itemStatus = "";

    static final class Result
    {
        final BufferedImage image;
        final boolean itemIcon, hidden, hideOverlay;
        final int menuSort;
        final Color menuColor;
        final BufferedImageProvider descriptor;
        final int itemId, quantity;
        Result(BufferedImage image, boolean itemIcon) { this(image,itemIcon,false,false,0,null); }
        Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int sort, Color color)
        { this(image,itemIcon,hidden,hideOverlay,sort,color,null,-1,1); }
        Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int sort, Color color,
               BufferedImageProvider descriptor, int itemId, int quantity)
        {
            this.image=image; this.itemIcon=itemIcon; this.hidden=hidden; this.hideOverlay=hideOverlay;
            this.menuSort=sort; this.menuColor=color; this.descriptor=descriptor; this.itemId=itemId; this.quantity=quantity;
        }
        boolean hasIcon() { return !hidden && !hideOverlay && (image!=null || descriptor!=null); }
    }

    String status() { return source.status() + (itemStatus.isEmpty() ? "" : " " + itemStatus) + " " + fileIcons.problem(); }
    long revision() { return source.revision(); }
    void refresh()
    {
        if (!client.isClientThread()) return;
        if (!config.lootFiltersCompatibility() && !config.useLootFiltersInMenu())
        { source.clear("Loot Filters compatibility is off."); return; }
        boolean running=false;
        for (Plugin plugin : plugins.getPlugins())
            if ("Loot Filters".equals(plugin.getName()) && plugins.isPluginActive(plugin)) { running=true; break; }
        if (!running)
        { source.clear("Enable the regular Loot Filters plugin to use its selected filter."); fileIcons.clear(); return; }
        source.refresh(LocalFilterSettings.read(configs));
    }
    void reset() { endMenu(); fileIcons.clear(); source.clear("Reading selected Loot Filters rules..."); itemStatus=""; }
    void beginMenu() { menuResults.clear(); collecting=true; itemStatus=""; refresh(); }
    void endMenu() { menuResults.clear(); collecting=false; }

    Result findIcon(MenuEntry entry)
    {
        Result result=find(entry);
        if (result!=null && result.hasIcon()) image(result); // Warm asynchronous files; never read them on this thread.
        return result;
    }

    BufferedImage image(Result result)
    {
        if (result==null || !result.hasIcon()) return null;
        if (result.image!=null) return result.image;
        BufferedImageProvider icon=result.descriptor;
        if (icon instanceof BufferedImageProvider.File)
            return fileIcons.get(((BufferedImageProvider.File)icon).getFilename());
        if (!client.isClientThread()) return null;
        if (icon instanceof BufferedImageProvider.CurrentItem)
            return items.getImage(result.itemId,result.quantity,false);
        if (icon instanceof BufferedImageProvider.Item)
            return items.getImage(((BufferedImageProvider.Item)icon).getItemId());
        if (icon instanceof BufferedImageProvider.Sprite)
        {
            BufferedImageProvider.Sprite sprite=(BufferedImageProvider.Sprite)icon;
            try { return sprites.getSprite(sprite.getSpriteId(),sprite.getIndex()); }
            catch (IndexOutOfBoundsException ex) { return null; }
        }
        return null;
    }

    Result find(MenuEntry entry)
    {
        if (!client.isClientThread()) return null;
        LocalLootFilterSource.Loaded loaded=source.current();
        if (loaded==null) return null;
        WorldView view=client.getWorldView(entry.getWorldViewId());
        if (view==null || view.getScene()==null) return null;
        Tile[][][] tiles=view.getScene().getTiles();
        int plane=view.getPlane(), x=entry.getParam0(), y=entry.getParam1();
        if (tiles==null || plane<0 || plane>=tiles.length || tiles[plane]==null || x<0
            || x>=tiles[plane].length || tiles[plane][x]==null || y<0 || y>=tiles[plane][x].length) return null;
        Tile tile=tiles[plane][x][y];
        if (tile==null) return null;
        if (!collecting) return evaluate(loaded,tile,entry.getIdentifier());
        Map<Integer,Result> results=menuResults.computeIfAbsent(tile,ignored -> new HashMap<>());
        if (!results.containsKey(entry.getIdentifier())) results.put(entry.getIdentifier(),evaluate(loaded,tile,entry.getIdentifier()));
        return results.get(entry.getIdentifier());
    }

    private Result evaluate(LocalLootFilterSource.Loaded loaded, Tile tile, int id)
    {
        if (id<0 || id>=client.getItemCount()) return null;
        List<TileItem> ground=tile.getGroundItems();
        if (ground==null) return null;
        LootFilter.Resolved chosen=null;
        int quantity=1;
        for (TileItem item : ground)
        {
            if (item.getId()!=id) continue;
            ItemComposition composition=items.getItemComposition(id);
            if (composition==null || composition.getName()==null) return null;
            ItemComposition note=composition.getLinkedNoteId()<0 ? null : items.getItemComposition(composition.getLinkedNoteId());
            // Match the released evaluator's instance/template coordinate interpretation.
            if (tile.getLocalLocation()==null || client.getWorldView(tile.getLocalLocation().getWorldView())==null) return null;
            WorldPoint location=WorldPoint.fromLocalInstance(client,tile.getLocalLocation());
            if (location==null) return null;
            long price=id==995 ? 1 : id==13204 ? 1000 : items.getItemPrice(id);
            ItemFacts facts=ItemFacts.builder().id(id).name(composition.getName()).quantity(item.getQuantity())
                .ownership(item.getOwnership()).accountType(client.getVarbitValue(VarbitID.IRONMAN))
                .gePrice(price).haPrice(composition.getHaPrice()).stackable(composition.isStackable())
                .noted(composition.getNote()!=-1).tradeable(composition.isTradeable() || note!=null && note.isTradeable())
                .geTradeable(composition.isGeTradeable() || note!=null && note.isGeTradeable()).worldPoint(location).build();
            LootFilter.Resolved display=loaded.program.resolve(facts,loaded.settings.isCompact());
            if (chosen!=null && (!chosen.equals(display) || display.getIcon() instanceof BufferedImageProvider.CurrentItem
                && quantity!=item.getQuantity()))
            { itemStatus="Different stacks resolve differently; their shared menu row has no custom icon."; return null; }
            chosen=display;
            quantity=item.getQuantity();
        }
        if (chosen==null) return null;
        BufferedImageProvider icon=chosen.visibleIcon();
        if (icon instanceof BufferedImageProvider.Item
            && ((BufferedImageProvider.Item)icon).getItemId()>=client.getItemCount()) icon=null;
        if (icon instanceof BufferedImageProvider.Sprite && ((BufferedImageProvider.Sprite)icon).getSpriteId()==9000000) icon=null;
        if (icon instanceof BufferedImageProvider.File)
        {
            try { IconImages.safeName(((BufferedImageProvider.File)icon).getFilename()); }
            catch (IllegalArgumentException ex) { icon=null; itemStatus="The filter selected an unsupported image filename."; }
        }
        boolean same=icon instanceof BufferedImageProvider.CurrentItem || icon instanceof BufferedImageProvider.Item
            && ((BufferedImageProvider.Item)icon).getItemId()==id;
        return new Result(null,same,chosen.isHidden(),chosen.isHideOverlay(),chosen.getMenuSort(),chosen.getMenuColor(),icon,id,quantity);
    }
}
