package com.improvedmenus;

import com.google.inject.*;
import java.awt.Canvas;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RevealAndRestorationTest
{
    private Client client; private ImprovedMenusConfig config; private MenuEngine engine; private RevealState reveal;
    @Before public void setup()
    {
        client=mock(Client.class); config=mock(ImprovedMenusConfig.class);
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
        when(config.itemRules()).thenReturn("Drop,*"); when(config.colors()).thenReturn("Sword:Use:#ff0000"); when(config.exceptions()).thenReturn("");
        when(client.getCanvas()).thenReturn(new Canvas() { @Override public boolean hasFocus() { return true; } });
        Injector injector=Guice.createInjector(new AbstractModule() { protected void configure() { bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); } });
        engine=injector.getInstance(MenuEngine.class); reveal=injector.getInstance(RevealState.class); engine.reload();
    }
    @Test public void ctrlRevealPausesOwnedFilters()
    {
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
        MenuEntry drop=TestMenus.entry("Drop","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY); Menu menu=TestMenus.menu(drop);
        engine.apply(menu); assertSame(drop,menu.getMenuEntries()[0]);
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(false); engine.apply(menu); assertEquals(0,menu.getMenuEntries().length);
    }
    @Test public void revealCannotRestoreOtherPluginsRemovedOptions()
    { when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true); Menu menu=TestMenus.menu(); engine.apply(menu); assertEquals(0,menu.getMenuEntries().length); }
    @Test public void lossOfCanvasFocusDisablesReveal()
    {
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true); when(client.getCanvas()).thenReturn(new Canvas() { @Override public boolean hasFocus() { return false; } });
        assertFalse(reveal.isRevealed());
    }
    @Test public void configurableModifierAndNoneWork()
    {
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.ALT); when(client.isKeyPressed(KeyCode.KC_ALT)).thenReturn(true); assertTrue(reveal.isRevealed());
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE); assertFalse(reveal.isRevealed());
    }
    @Test public void clearingRestoresExactOriginalOptionIncludingOtherFormatting()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("<img=2><col=00ff00>Use</col>","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); assertTrue(e.getOption().contains("ff0000")); engine.clear(); assertEquals("<img=2><col=00ff00>Use</col>",e.getOption());
    }
    @Test public void anotherPluginsLaterColorIsNotOverwrittenOnShutdown()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); e.setOption("<col=0000ff>Use</col>"); engine.clear(); assertEquals("<col=0000ff>Use</col>",e.getOption());
    }
    @Test public void remappedEntryIsNotRestored()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); e.setTarget("Different item"); engine.clear(); assertTrue(e.getOption().contains("ff0000"));
    }
    @Test public void configChangeRestoresColorBeforeRebuildingRules()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); when(config.enableColors()).thenReturn(false); engine.reload(); assertEquals("Use",e.getOption());
    }
}
