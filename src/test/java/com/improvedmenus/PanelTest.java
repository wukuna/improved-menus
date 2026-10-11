package com.improvedmenus;

import com.google.gson.Gson;
import com.google.inject.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PanelTest
{
    @Test public void editorRendersAndCanStopItsTimer() throws Exception
    {
        Client client=mock(Client.class); ImprovedMenusConfig config=mock(ImprovedMenusConfig.class); ConfigManager manager=mock(ConfigManager.class);
        Injector injector=Guice.createInjector(new AbstractModule() { protected void configure() {
            bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class)); bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); bind(ConfigManager.class).toInstance(manager); bind(Gson.class).toInstance(new Gson());
        } });
        SwingUtilities.invokeAndWait(() -> {
            ImprovedMenusPanel panel=new ImprovedMenusPanel(injector.getInstance(ProfileStore.class),injector.getInstance(MenuEngine.class));
            panel.setSize(225,850); layout(panel);
            BufferedImage image=new BufferedImage(225,850,BufferedImage.TYPE_INT_RGB); Graphics2D g=image.createGraphics(); panel.printAll(g); g.dispose();
            try { ImageIO.write(image,"png",new File("build/ui-preview.png")); } catch (java.io.IOException e) { throw new AssertionError(e); }
            assertTrue(panel.getComponentCount() >= 3); panel.stop();
        });
    }
    private static void layout(Container container) { container.doLayout(); for (Component c : container.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
}
