package com.improvedmenus;

import com.improvedmenus.rs2f.LootFilter;
import com.improvedmenus.rs2f.model.BufferedImageProvider;
import com.improvedmenus.rs2f.model.ItemFacts;
import java.awt.Color;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocalFilterSettingsTest
{
    static ItemFacts item(String name,int quantity)
    { return ItemFacts.builder().id(385).name(name).quantity(quantity).accountType(1).ownership(1).gePrice(1000)
        .haPrice(50).worldPoint(new WorldPoint(3200,3200,0)).build(); }
    @Test public void highlightBeforeHideAndScriptMatchesReleasedPrecedence()
    {
        LootFilter script=LootFilter.compile("rule(true){icon=Sprite(107,0); menuSort=9;}");
        LocalFilterSettings settings=new LocalFilterSettings("a.rs2f",false,"Shark>2","Shark",Color.GREEN,Color.BLUE,17);
        LootFilter decorated=settings.decorate(script);
        LootFilter.Resolved highlighted=decorated.resolve(item("Shark",3),false);
        assertFalse(highlighted.isHidden()); assertNull(highlighted.visibleIcon());
        assertEquals(Color.BLUE,highlighted.getMenuColor()); assertEquals(17,highlighted.getMenuSort());
        assertTrue(decorated.resolve(item("Shark",2),false).isHidden());
        assertEquals(new BufferedImageProvider.Sprite(107,0),decorated.resolve(item("Lobster",1),false).visibleIcon());
    }
    @Test public void compactAndVisibilityKeepTheirDistinctMeanings()
    {
        LootFilter program=LootFilter.compile("apply(true){icon=Item(995); hidden=true;} rule(true){hidden=false;hideOverlay=true;}");
        assertFalse(program.resolve(item("Shark",1),false).isHidden());
        assertTrue(program.resolve(item("Shark",1),false).isHideOverlay());
        assertNull(program.resolve(item("Shark",1),false).visibleIcon());
        LocalFilterSettings settings=new LocalFilterSettings(null,true,"Shark","Shark",Color.GREEN,null,0);
        assertEquals(new BufferedImageProvider.CurrentItem(),settings.decorate(program).resolve(item("Shark",1),true).visibleIcon());
        assertEquals(new Color(0xff9040),LootFilter.compile("").resolve(item("Shark",1),false).getMenuColor());
    }
    @Test public void readingForeignPreferencesNeverWritesOrDefaultsTheirConfig()
    {
        ConfigManager configs=mock(ConfigManager.class);
        when(configs.getConfiguration("loot-filters","selected-filter")).thenReturn("a.rs2f");
        when(configs.getConfiguration("loot-filters","hdMenuSort")).thenReturn("broken");
        LocalFilterSettings settings=LocalFilterSettings.read(configs);
        assertEquals("a.rs2f",settings.getSelected()); assertFalse(settings.isCompact());
        assertEquals(new Color(0xaa00ff),settings.getHighlightColor()); assertEquals(0,settings.getHighlightSort());
        verify(configs,never()).setConfiguration(anyString(),anyString(),any());
        verify(configs,never()).unsetConfiguration(anyString(),anyString());
    }
}
