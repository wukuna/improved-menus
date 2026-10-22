package com.improvedmenus;
import java.awt.image.BufferedImage;
import java.util.Map;
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
        Provider(EventBus bus){this.bus=bus;}
        @Subscribe public void onPluginMessage(PluginMessage message) {
            if(!message.getName().equals("menu-icon-request-v1") || !reply) return;
            bus.post(new PluginMessage("loot-filters","menu-icon-response-v1",
                Map.of("requestId","other-consumer","status","ok","icon",image)));
            bus.post(new PluginMessage("loot-filters","menu-icon-response-v1",
                Map.of("requestId",message.getData().get("requestId"),"status","ok","icon",image,"itemIcon",false)));
        }
    }
    @Test public void synchronousReplyAndProviderDisableCannotLeaveStaleIcons() {
        EventBus bus=new EventBus();
        LootFiltersCompatibility bridge=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule(){
            protected void configure(){bind(EventBus.class).toInstance(bus); bind(net.runelite.api.Client.class).toInstance(mock(net.runelite.api.Client.class));}
        }).getInstance(LootFiltersCompatibility.class);
        bus.register(bridge); Tile tile=mock(Tile.class);
        assertNull(bridge.request(tile,385));
        Provider provider=new Provider(bus); bus.register(provider);
        LootFiltersCompatibility.Result result=bridge.request(tile,385);
        assertNotNull(result); assertSame(provider.image,result.image); assertFalse(result.itemIcon);
        bus.unregister(provider);
        assertNull(bridge.request(tile,385));
        assertTrue(bridge.status().contains("not responding"));
        bridge.onPluginMessage(new PluginMessage("loot-filters","menu-icon-response-v1",Map.of("requestId","late","status","ok","icon",provider.image)));
        assertNull(bridge.request(tile,385)); bus.unregister(bridge);
    }
}
