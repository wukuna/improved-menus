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
            java.util.List<JButton> groups=components(panel,JButton.class);
            groups.removeIf(b -> b.getName()==null || !b.getName().startsWith("section:"));
            assertEquals("section:Bank",groups.get(groups.size()-1).getName());
            JButton colors=groups.stream().filter(b -> b.getName().equals("section:Colors")).findFirst().get();
            assertTrue(colors.getText().startsWith("▶"));
            colors.doClick();
            assertTrue(components(panel,JButton.class).stream().anyMatch(b -> "Default picker color".equals(b.getText())));
            JTextField search=components(panel,JTextField.class).get(0);
            search.setText("minimum");
            assertEquals(1,components(panel,JSpinner.class).size());
            assertTrue(components(panel,JButton.class).stream().filter(b -> "section:Presentation".equals(b.getName())).allMatch(b -> !b.isEnabled()));
            search.setText("");
            assertEquals(0,components(panel,JSpinner.class).size());
            assertTrue(components(panel,JButton.class).stream().anyMatch(b -> "Default picker color".equals(b.getText())));
            search.setText("minimum");
            components(panel,JButton.class).stream().filter(b -> "Reset shown".equals(b.getText())).findFirst().get().doClick();
            verify(manager).setConfiguration("improvedmenus","minimumOptions","1");
            verify(manager,never()).setConfiguration(eq("improvedmenus"),eq("defaultPickerColor"),anyString());
            search.setText("");
            panel.setSize(225,850); layout(panel);
            BufferedImage image=new BufferedImage(225,850,BufferedImage.TYPE_INT_RGB); Graphics2D g=image.createGraphics(); panel.printAll(g); g.dispose();
            try { ImageIO.write(image,"png",new File("build/ui-preview.png")); } catch (java.io.IOException e) { throw new AssertionError(e); }
            assertTrue(panel.getComponentCount() >= 3); panel.stop();
        });
    }
    private static void layout(Container container) { container.doLayout(); for (Component c : container.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
    private static <T extends Component> java.util.List<T> components(Container container,Class<T> type)
    {
        java.util.List<T> result=new java.util.ArrayList<>();
        for (Component c : container.getComponents()) { if (type.isInstance(c)) { result.add(type.cast(c)); } if (c instanceof Container) { result.addAll(components((Container)c,type)); } }
        return result;
    }
}
