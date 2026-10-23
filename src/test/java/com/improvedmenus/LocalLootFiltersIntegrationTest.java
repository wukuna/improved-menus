package com.improvedmenus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Real reader + evaluator + menu rendering, with game API state supplied by a controlled fixture. */
public class LocalLootFiltersIntegrationTest
{
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    final Client client=mock(Client.class);
    final ImprovedMenusConfig config=mock(ImprovedMenusConfig.class,CALLS_REAL_METHODS);
    final ConfigManager configs=mock(ConfigManager.class);
    final ItemManager items=mock(ItemManager.class);
    final SpriteManager sprites=mock(SpriteManager.class);
    final PluginManager plugins=mock(PluginManager.class);
    final Plugin regular=mock(Plugin.class);
    final LootFilterFileIconsTest.ManualExecutor worker=new LootFilterFileIconsTest.ManualExecutor();
    LootFiltersCompatibility compatibility;
    final Tile tile=mock(Tile.class);
    final TileItem item=mock(TileItem.class);
    Filepath root;
    MenuPresentation presentation;

    @Before public void setup() throws Exception
    {
        root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        when(client.isClientThread()).thenReturn(true); when(client.getItemCount()).thenReturn(40000);
        when(config.lootFiltersCompatibility()).thenReturn(true); when(config.groundIcons()).thenReturn(true);
        when(config.groundIconSource()).thenReturn(ImprovedMenusConfig.GroundIconSource.CUSTOM);
        when(regular.getName()).thenReturn("Loot Filters"); when(plugins.getPlugins()).thenReturn(List.of(regular));
        when(plugins.isPluginActive(regular)).thenReturn(true);
        when(configs.getConfiguration("loot-filters","selected-filter")).thenReturn("mine.rs2f");
        WorldView view=mock(WorldView.class); Scene scene=mock(Scene.class);
        when(client.getWorldView(0)).thenReturn(view); when(client.getTopLevelWorldView()).thenReturn(view);
        when(view.getScene()).thenReturn(scene); when(view.getBaseX()).thenReturn(3200); when(view.getBaseY()).thenReturn(3200);
        when(scene.getTiles()).thenReturn(new Tile[][][]{{{tile}}});
        when(tile.getLocalLocation()).thenReturn(new LocalPoint(64,64,0));
        when(tile.getGroundItems()).thenReturn(List.of(item)); when(item.getId()).thenReturn(385); when(item.getQuantity()).thenReturn(2);
        ItemComposition composition=mock(ItemComposition.class);
        when(composition.getName()).thenReturn("Shark"); when(composition.getLinkedNoteId()).thenReturn(-1); when(composition.getNote()).thenReturn(-1);
        when(items.getItemComposition(385)).thenReturn(composition); when(items.getItemPrice(385)).thenReturn(1000L);
        compatibility=new LootFiltersCompatibility(client,configs,config,plugins,items,sprites,
            new LootFilterFileIcons(worker,()->0,LootFilterFileIcons::readImage,root),
            new LocalLootFilterSource(worker,()->0,name->LocalLootFilterSource.read(root,name)));
        ChatIconManager icons=mock(ChatIconManager.class); when(icons.chatIconIndex(anyInt())).thenReturn(4);
        presentation=Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config);
                bind(ItemManager.class).toInstance(items); bind(ChatIconManager.class).toInstance(icons);
                bind(LootFiltersCompatibility.class).toInstance(compatibility);
                bind(RuneLiteConfig.class).toInstance(mock(RuneLiteConfig.class));
                bind(TooltipManager.class).toInstance(mock(TooltipManager.class));
            }
        }).getInstance(MenuPresentation.class);
        presentation.start();
    }
    void script(String text) throws Exception
    {
        root.joinSegment("mine.rs2f").write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        compatibility.reset(); compatibility.refresh(); worker.run();
        assertNotNull(compatibility.status(),compatibility.source.current());
    }
    MenuEntry take()
    { return UserCombinationAuditTest.e("Take","<col=ff0000>Shark</col>",MenuAction.GROUND_ITEM_THIRD_OPTION,385,0); }

    @Test public void fileIconAppearsInSameOpenMenuWithoutAnyProviderMessages() throws Exception
    {
        LootFilterFileIconsTest.write(root.joinSegment("heart.png"),Color.MAGENTA);
        script("rule(id:385){icon=File(\"heart.png\");}");
        MenuEntry entry=take(); Menu menu=UserCombinationAuditTest.menu(entry);
        when(client.getMenu()).thenReturn(menu); when(client.isMenuOpen()).thenReturn(true);
        when(menu.getMenuWidth()).thenReturn(200); when(menu.getMenuHeight()).thenReturn(80);
        presentation.decorate(menu);
        assertEquals("<img=4><col=ff0000>Shark</col>",entry.getTarget());
        assertNull(compatibility.image(compatibility.find(entry)));
        worker.run();
        BufferedImage canvas=new BufferedImage(250,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=canvas.createGraphics(); try { presentation.render(g); } finally { g.dispose(); }
        boolean painted=false;
        for(int y=0;y<100;y++) for(int x=0;x<250;x++) painted|=canvas.getRGB(x,y)==Color.MAGENTA.getRGB();
        assertTrue("New image must appear without reopening the menu",painted);
        presentation.stop(); assertEquals("<col=ff0000>Shark</col>",entry.getTarget());
        verify(configs,never()).setConfiguration(anyString(),anyString(),any());
        verify(plugins,never()).startPlugin(any()); verify(plugins,never()).stopPlugin(any());
    }
    @Test public void spriteItemAndCurrentItemComeFromOurCoreManagers() throws Exception
    {
        BufferedImage sprite=new BufferedImage(17,15,2); when(sprites.getSprite(107,0)).thenReturn(sprite);
        script("rule(true){icon=Sprite(107,0);}"); assertSame(sprite,compatibility.image(compatibility.find(take())));
        script("rule(true){icon=Item(995);}"); compatibility.image(compatibility.find(take())); verify(items).getImage(995);
        script("rule(true){icon=CurrentItem();}"); LootFiltersCompatibility.Result result=compatibility.find(take());
        assertTrue(result.itemIcon); compatibility.image(result); verify(items).getImage(385,2,false);
    }
    @Test public void hideListsCompactAndDuplicateStacksAreEvaluatedLocally() throws Exception
    {
        when(configs.getConfiguration("loot-filters","hiddenItems")).thenReturn("Shark");
        script("rule(true){icon=Sprite(107,0);}"); assertFalse(compatibility.find(take()).hasIcon());
        when(configs.getConfiguration("loot-filters","highlightedItems")).thenReturn("Shark");
        when(configs.getConfiguration("loot-filters","compactMode")).thenReturn("true");
        compatibility.reset(); compatibility.refresh(); worker.run(); assertTrue(compatibility.find(take()).itemIcon);
        TileItem other=mock(TileItem.class); when(other.getId()).thenReturn(385); when(other.getQuantity()).thenReturn(1);
        when(tile.getGroundItems()).thenReturn(List.of(item,other)); assertNull(compatibility.find(take()));
        assertTrue(compatibility.status().contains("Different stacks"));
    }
    @Test public void regularPluginDisableAndSourceSwitchDoNotLeaveResolvedIcons() throws Exception
    {
        script("rule(true){icon=Sprite(107,0);}"); assertNotNull(compatibility.find(take()));
        when(plugins.isPluginActive(regular)).thenReturn(false); compatibility.refresh(); assertNull(compatibility.find(take()));
        when(plugins.isPluginActive(regular)).thenReturn(true); compatibility.refresh(); worker.run(); assertNotNull(compatibility.find(take()));
        when(configs.getConfiguration("loot-filters","selected-filter")).thenReturn("missing.rs2f");
        compatibility.refresh(); assertNull(compatibility.find(take())); worker.run(); assertNull(compatibility.find(take()));
        assertTrue(compatibility.status().contains("could not load"));
    }
}
