package com.improvedmenus;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.Tile;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class LootFiltersCompatibilityTest {
    static class Provider {
        final EventBus bus; final BufferedImage image=new BufferedImage(16,16,2);
        boolean reply=true;
        int requests;
        Provider(EventBus bus){this.bus=bus;}
        @Subscribe public void onPluginMessage(PluginMessage message) {
            if(!message.getName().equals("resolved-display-request-v1") || !reply) return;
            requests++;
            Map<String,Object> data=response(image);
            data.put("requestId","other-consumer");
            bus.post(new PluginMessage("loot-filters","resolved-display-response-v1",data));
            data.put("requestId",message.getData().get("requestId"));
            bus.post(new PluginMessage("loot-filters","resolved-display-response-v1",data));
        }
    }
    @Test public void synchronousReplyAndProviderDisableCannotLeaveStaleIcons() {
        EventBus bus=new EventBus();
        Client client=mock(Client.class); when(client.isClientThread()).thenReturn(true);
        LootFiltersCompatibility bridge=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule(){
            protected void configure(){bind(EventBus.class).toInstance(bus); bind(Client.class).toInstance(client);}
        }).getInstance(LootFiltersCompatibility.class);
        bus.register(bridge); Tile tile=mock(Tile.class);
        assertNull(bridge.request(tile,385));
        Provider provider=new Provider(bus); bus.register(provider);
        LootFiltersCompatibility.Result result=bridge.request(tile,385);
        assertNotNull(result); assertSame(provider.image,result.image); assertFalse(result.itemIcon);
        bus.unregister(provider);
        assertNull(bridge.request(tile,385));
        assertTrue(bridge.status().contains("not responding"));
        bridge.onPluginMessage(new PluginMessage("loot-filters","resolved-display-response-v1",Map.of("requestId","late","status","ok","icon",provider.image)));
        assertNull(bridge.request(tile,385)); bus.unregister(bridge);
    }
    static Map<String,Object> response(BufferedImage image) {
        Map<String,Object> data=new HashMap<>();
        data.put("status","ok"); data.put("display",Map.of("hidden",false,"hideOverlay",false));
        data.put("menu",Map.of("sort",7,"deprioritized",false,"textColor",java.awt.Color.CYAN));
        data.put("iconStatus",image==null ? "none" : "ready"); if(image!=null) data.put("icon",image);
        data.put("itemIcon",false); return data;
    }
    @Test public void displaySurvivesMissingOrHiddenImagesAndMalformedRepliesFailOpen() {
        Map<String,Object> data=response(null);
        LootFiltersCompatibility.Result result=LootFiltersCompatibility.decode(data);
        assertNotNull(result); assertNull(result.image); assertEquals(7,result.menuSort); assertEquals(java.awt.Color.CYAN,result.menuColor);
        data.put("iconStatus","not-ready"); assertNotNull(LootFiltersCompatibility.decode(data));
        data.put("iconStatus","hidden"); data.put("display",Map.of("hidden",true,"hideOverlay",true));
        result=LootFiltersCompatibility.decode(data); assertTrue(result.hidden); assertNull(result.image);
        data=response(new BufferedImage(16,16,2)); data.put("display",Map.of("hidden",true,"hideOverlay",true));
        assertNull(LootFiltersCompatibility.decode(data).image);
        data.put("menu",Map.of("sort","wrong")); assertNull(LootFiltersCompatibility.decode(data));
        for(String status:new String[]{"not-found","not-ready","ambiguous","future"}) {
            data=response(null); data.put("status",status); assertNull(LootFiltersCompatibility.decode(data));
        }
    }
    @Test public void cachesOnlyDuringOneMenuCallbackAndKeepsWorldViewsSeparate() {
        EventBus bus=new EventBus(); Client client=mock(Client.class); when(client.isClientThread()).thenReturn(true);
        WorldView first=mock(WorldView.class),second=mock(WorldView.class); Scene a=mock(Scene.class),b=mock(Scene.class);
        when(client.getWorldView(0)).thenReturn(first); when(client.getWorldView(1)).thenReturn(second);
        when(first.getScene()).thenReturn(a); when(second.getScene()).thenReturn(b);
        when(a.getTiles()).thenReturn(new Tile[][][]{{{mock(Tile.class)}}}); when(b.getTiles()).thenReturn(new Tile[][][]{{{mock(Tile.class)}}});
        LootFiltersCompatibility bridge=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule(){
            protected void configure(){bind(EventBus.class).toInstance(bus); bind(Client.class).toInstance(client);}
        }).getInstance(LootFiltersCompatibility.class);
        Provider provider=new Provider(bus); bus.register(bridge); bus.register(provider);
        MenuEntry entry=mock(MenuEntry.class); when(entry.getIdentifier()).thenReturn(385);
        bridge.beginMenu(); assertNotNull(bridge.find(entry)); assertNotNull(bridge.find(entry)); assertEquals(1,provider.requests);
        when(entry.getWorldViewId()).thenReturn(1); assertNotNull(bridge.find(entry)); assertEquals(2,provider.requests);
        bridge.endMenu(); provider.reply=false; bridge.beginMenu(); assertNull(bridge.find(entry)); assertNull(bridge.find(entry)); bridge.endMenu();
        provider.reply=true; assertNotNull(bridge.find(entry)); assertEquals(3,provider.requests);
        bridge.beginMenu(); bridge.find(entry); bridge.reset(); provider.reply=false; assertNull(bridge.find(entry));
        when(client.isClientThread()).thenReturn(false); assertNull(bridge.find(entry)); assertNull(bridge.request(mock(Tile.class),385));
    }
}
