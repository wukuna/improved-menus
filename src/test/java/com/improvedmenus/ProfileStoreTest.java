package com.improvedmenus;

import com.google.gson.Gson;
import com.google.inject.*;
import java.util.*;
import net.runelite.client.config.ConfigManager;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProfileStoreTest
{
    private ProfileStore store;
    private ConfigManager manager;
    private final Map<String,String> values=new HashMap<>();
    @Before public void setup()
    {
        manager=mock(ConfigManager.class);
        when(manager.getConfiguration(anyString(),anyString())).thenAnswer(i -> values.get(i.getArgument(0)+"/"+i.getArgument(1)));
        doAnswer(i -> { values.put(i.getArgument(0)+"/"+i.getArgument(1),i.getArgument(2).toString()); return null; }).when(manager).setConfiguration(anyString(),anyString(),any());
        store=Guice.createInjector(new AbstractModule() { protected void configure() { bind(ConfigManager.class).toInstance(manager); bind(Gson.class).toInstance(new Gson()); } }).getInstance(ProfileStore.class);
    }
    @Test public void roundTripProfiles()
    { Map<String,String> initial=store.snapshot(); assertEquals(initial,store.previewImport(store.exportSettings(initial))); }
    @Test public void importPreviewDoesNotWrite()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":\"true\"}}"); assertTrue(values.isEmpty()); }
    @Test public void applyAndUndo()
    { store.apply(Collections.singletonMap("bankEat","true")); assertEquals("true",values.get("improvedmenus/bankEat")); assertTrue(store.undo()); assertEquals("false",values.get("improvedmenus/bankEat")); }
    @Test public void entireImportValidatedBeforeAnyWrite()
    {
        Map<String,String> settings=new LinkedHashMap<>(); settings.put("bankEat","true"); settings.put("itemRules","Invalid");
        try { store.apply(settings); fail(); } catch (IllegalArgumentException expected) { assertTrue(values.isEmpty()); }
    }
    @Test(expected=IllegalArgumentException.class) public void cannotImportOtherPluginKeys()
    { store.previewImport("{\"version\":1,\"settings\":{\"script\":\"danger\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void invalidBooleanRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":\"yes\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void invalidModeRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"report\":\"AUTO\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void futureVersionRejected()
    { store.previewImport("{\"version\":2,\"settings\":{}}"); }
    @Test(expected=IllegalArgumentException.class) public void nonStringValueRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":true}}"); }
    @Test(expected=IllegalArgumentException.class) public void malformedJsonRejected()
    { store.previewImport("not JSON"); }
    @Test public void namedProfilesPersistIndependentlyOfCurrentSettings()
    {
        store.apply(Collections.singletonMap("bankEat","true")); store.save("Banking"); store.apply(Collections.singletonMap("bankEat","false"));
        assertEquals("true",store.load("Banking").get("bankEat")); assertEquals(Collections.singletonList("Banking"),store.names());
    }
    @Test public void selectiveResetLeavesOtherSettings()
    {
        store.apply(Collections.singletonMap("bankEat","true")); store.apply(Collections.singletonMap("bankWield","true"));
        store.apply(Collections.singletonMap("bankEat","false")); assertEquals("true",store.snapshot().get("bankWield"));
    }
    @Test public void historyBoundedToTwentyEdits()
    { for (int i=0;i<30;i++) { store.apply(Collections.singletonMap("bankEat",i%2==0 ? "true" : "false")); } int n=0; while (store.undo()) { n++; } assertEquals(20,n); }
    @Test public void legacyImportReadsExactPublishedKeysAndLeavesSourceAlone()
    {
        values.put("noexamine/examineItemGround","true"); values.put("noexamine/examineItemsInventory","false");
        Map<String,String> imported=store.legacyPreview("No Examine"); assertEquals("true",imported.get("examineGround")); assertEquals("false",imported.get("examineInventory"));
        store.apply(imported); assertEquals("true",values.get("noexamine/examineItemGround"));
    }
    @Test public void toolConfigUsesHyphenatedSourceGroup()
    { values.put("tool-required/chopDown","true"); assertEquals("true",store.legacyPreview("Tool Required").get("chop")); }
    @Test public void noSavedLegacySettingsProducesEmptyPreview()
    { assertTrue(store.legacyPreview("Menu Colorizer").isEmpty()); }
    @Test public void clearHistoryDoesNotDeleteProfiles()
    { store.save("Normal"); store.apply(Collections.singletonMap("bankEat","true")); store.clearHistory(); assertFalse(store.undo()); assertEquals(1,store.names().size()); }
}
