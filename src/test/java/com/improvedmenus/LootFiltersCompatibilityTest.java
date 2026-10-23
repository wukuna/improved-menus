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
        final BufferedImage image=new BufferedImage(16,16,2);
        boolean reply=true;
        int requests;
        Map<String,Object> lastRequest;
        Provider(EventBus bus) {}
        @Subscribe public void onPluginMessage(PluginMessage message) {
            if(!message.getName().equals("resolved-display-request-v1") || !reply) return;
            requests++;
            lastRequest=new HashMap<>(message.getData());
            assertFalse(message.getData().containsKey("tile"));
            assertFalse(message.getData().containsKey("requestId"));
            for(String field:new String[]{"x","y","z","itemId","worldViewId"}) assertTrue(message.getData().get(field) instanceof Integer);
            message.getData().putAll(response(image));
        }
    }
    @Test public void synchronousReplyAndProviderDisableCannotLeaveStaleIcons() {
        EventBus bus=new EventBus();
        Client client=mock(Client.class); when(client.isClientThread()).thenReturn(true);
        LootFiltersCompatibility bridge=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule(){
            protected void configure(){bind(EventBus.class).toInstance(bus); bind(Client.class).toInstance(client);}
        }).getInstance(LootFiltersCompatibility.class);
        WorldView view=mock(WorldView.class); when(view.getId()).thenReturn(8);
        when(view.getBaseX()).thenReturn(3200); when(view.getBaseY()).thenReturn(6400);
        assertNull(bridge.request(view,0,0,0,385));
        Provider provider=new Provider(bus); bus.register(provider);
        LootFiltersCompatibility.Result result=bridge.request(view,0,0,0,385);
        assertNotNull(result); assertSame(provider.image,result.image); assertFalse(result.itemIcon);
        assertEquals(Map.of("worldViewId",8,"x",3200,"y",6400,"z",0,"itemId",385),provider.lastRequest);
        bus.unregister(provider);
        assertNull(bridge.request(view,0,0,0,385));
        assertTrue(bridge.status().contains("not responding"));
        assertNull(bridge.request(view,0,0,0,385));
    }
    static Map<String,Object> response(BufferedImage image) {
        Map<String,Object> display=new HashMap<>();
        display.put("hidden",false); display.put("hideOverlay",false);
        display.put("menuSort",7); display.put("menuTextColor",java.awt.Color.CYAN);
        display.put("icon",image==null ? null : Map.of("type","file","filename","custom.png","image",image));
        return new HashMap<>(Map.of("status","ok","display",display));
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> display(Map<String,Object> data) { return (Map<String,Object>)data.get("display"); }
    @Test public void displaySurvivesMissingOrHiddenImagesAndMalformedRepliesFailOpen() {
        Map<String,Object> data=response(null);
        LootFiltersCompatibility.Result result=LootFiltersCompatibility.decode(data,385);
        assertNotNull(result); assertNull(result.image); assertEquals(7,result.menuSort); assertEquals(java.awt.Color.CYAN,result.menuColor);
        display(data).put("icon",Map.of("type","file","filename","pending.png"));
        assertNotNull(LootFiltersCompatibility.decode(data,385)); assertNull(LootFiltersCompatibility.decode(data,385).image);
        data=response(new BufferedImage(16,16,2)); display(data).put("hidden",true); display(data).put("hideOverlay",true);
        result=LootFiltersCompatibility.decode(data,385); assertTrue(result.hidden); assertNull(result.image);
        display(data).put("hidden",false); assertNull(LootFiltersCompatibility.decode(data,385).image);
        display(data).put("menuSort","wrong"); assertNull(LootFiltersCompatibility.decode(data,385));
        for(Object icon:new Object[]{"wrong",Map.of("type","file","image","wrong")}) {
            data=response(null); display(data).put("icon",icon); assertNull(LootFiltersCompatibility.decode(data,385));
        }
        for(String status:new String[]{"not-found","not-ready","ambiguous","future"}) {
            data=response(null); data.put("status",status); assertNull(LootFiltersCompatibility.decode(data,385));
        }
    }
    @Test public void nativeDeduplicationIsDerivedFromTheIconDescriptor() {
        BufferedImage image=new BufferedImage(16,16,2);
        for(String type:new String[]{"item","current-item"}) {
            Map<String,Object> data=response(image); display(data).put("icon",Map.of("type",type,"id",385,"image",image));
            assertTrue(LootFiltersCompatibility.decode(data,385).itemIcon);
            assertFalse(LootFiltersCompatibility.decode(data,4151).itemIcon);
        }
        assertFalse(LootFiltersCompatibility.decode(response(image),385).itemIcon);
    }
    @Test public void cachesOnlyDuringOneMenuCallbackAndKeepsWorldViewsSeparate() {
        EventBus bus=new EventBus(); Client client=mock(Client.class); when(client.isClientThread()).thenReturn(true);
        WorldView first=mock(WorldView.class),second=mock(WorldView.class); Scene a=mock(Scene.class),b=mock(Scene.class);
        when(client.getWorldView(0)).thenReturn(first); when(client.getWorldView(1)).thenReturn(second);
        when(first.getScene()).thenReturn(a); when(second.getScene()).thenReturn(b);
        when(first.getId()).thenReturn(0); when(second.getId()).thenReturn(1);
        when(a.getTiles()).thenReturn(new Tile[][][]{{{mock(Tile.class)}}}); when(b.getTiles()).thenReturn(new Tile[][][]{{{mock(Tile.class)}}});
        LootFiltersCompatibility bridge=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule(){
            protected void configure(){bind(EventBus.class).toInstance(bus); bind(Client.class).toInstance(client);}
        }).getInstance(LootFiltersCompatibility.class);
        Provider provider=new Provider(bus); bus.register(provider);
        MenuEntry entry=mock(MenuEntry.class); when(entry.getIdentifier()).thenReturn(385);
        bridge.beginMenu(); assertNotNull(bridge.find(entry)); assertNotNull(bridge.find(entry)); assertEquals(1,provider.requests);
        when(entry.getWorldViewId()).thenReturn(1); assertNotNull(bridge.find(entry)); assertEquals(2,provider.requests);
        assertEquals(1,provider.lastRequest.get("worldViewId"));
        bridge.endMenu(); provider.reply=false; bridge.beginMenu(); assertNull(bridge.find(entry)); assertNull(bridge.find(entry)); bridge.endMenu();
        provider.reply=true; assertNotNull(bridge.find(entry)); assertEquals(3,provider.requests);
        bridge.beginMenu(); bridge.find(entry); bridge.reset(); provider.reply=false; assertNull(bridge.find(entry));
        when(client.isClientThread()).thenReturn(false); assertNull(bridge.find(entry)); assertNull(bridge.request(first,0,0,0,385));
    }
}
