package com.improvedmenus;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LocalLootFilterSourceTest
{
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    final LootFilterFileIconsTest.ManualExecutor worker=new LootFilterFileIconsTest.ManualExecutor();
    final AtomicLong clock=new AtomicLong();
    static LocalFilterSettings settings(String name)
    { return new LocalFilterSettings(name,false,"","",java.awt.Color.MAGENTA,null,0); }

    @Test public void staleReadsCannotCrossSelectionsProfilesOrDisable()
    {
        LocalLootFilterSource source=new LocalLootFilterSource(worker,clock::get,name -> "rule(true){icon=Item(995);}");
        source.refresh(settings("one.rs2f")); assertNull(source.current()); assertEquals(1,worker.tasks.size());
        source.refresh(settings("two.rs2f")); worker.run(); assertNull(source.current());
        source.refresh(settings("two.rs2f")); worker.run(); assertEquals("two.rs2f",source.current().settings.getSelected());
        source.clear("Disabled"); assertNull(source.current());
        source.refresh(settings("one.rs2f")); source.clear("Profile changed"); worker.run(); assertNull(source.current());
        assertEquals("Profile changed",source.status());
    }

    @Test public void diskReloadReusesUnchangedProgramAndDropsInvalidOldResults()
    {
        String[] script={"rule(true){icon=Item(995);}"};
        LocalLootFilterSource source=new LocalLootFilterSource(worker,clock::get,name -> script[0]);
        source.refresh(settings("one.rs2f")); worker.run();
        LocalLootFilterSource.Loaded first=source.current(); long revision=source.revision();
        source.refresh(settings("one.rs2f")); assertTrue(worker.tasks.isEmpty());
        clock.addAndGet(LocalLootFilterSource.POLL_NANOS); source.refresh(settings("one.rs2f")); worker.run();
        assertSame(first,source.current()); assertEquals(revision,source.revision());
        script[0]="rule(true){icon=Sprite(107,0);}";
        clock.addAndGet(LocalLootFilterSource.POLL_NANOS); source.refresh(settings("one.rs2f")); worker.run();
        assertNotSame(first,source.current()); assertTrue(source.revision()>revision);
        script[0]="broken";
        clock.addAndGet(LocalLootFilterSource.POLL_NANOS); source.refresh(settings("one.rs2f")); worker.run();
        assertNull(source.current()); assertTrue(source.status().contains("could not load"));
    }

    @Test public void selectedFileIsReadOnlyBoundedAndCannotEscape() throws Exception
    {
        Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        byte[] contents="rule(true){icon=File(\"heart.png\");}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(temporary.getRoot().toPath().resolve("My filter.rs2f"),contents);
        assertEquals(new String(contents,java.nio.charset.StandardCharsets.UTF_8),LocalLootFilterSource.read(root,"My filter.rs2f"));
        for(String name:new String[]{"../outside.rs2f","..\\outside.rs2f","C:/outside.rs2f","https://x/filter.rs2f","NUL.rs2f",".hidden.rs2f"})
        {
            try { LocalLootFilterSource.read(root,name); fail(name); }
            catch(IllegalArgumentException | IOException expected) { }
        }
        assertArrayEquals(contents,Files.readAllBytes(temporary.getRoot().toPath().resolve("My filter.rs2f")));
        assertEquals(1,temporary.getRoot().list().length);
        Files.write(temporary.getRoot().toPath().resolve("large.rs2f"),new byte[LocalLootFilterSource.MAX_BYTES+1]);
        try { LocalLootFilterSource.read(root,"large.rs2f"); fail(); } catch(IOException expected) { }
    }

    @Test public void allReleasedDefaultsLoadLocallyAndCompile() throws Exception
    {
        Filepath empty=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        for(String selected:LocalLootFilterSource.DEFAULTS.keySet())
        {
            String source=LocalLootFilterSource.read(empty,selected);
            assertTrue(selected,com.improvedmenus.rs2f.LootFilter.compile(source).rules.size()>20);
        }
        assertEquals(0,temporary.getRoot().list().length);
        assertEquals("",LocalLootFilterSource.read(empty,null));
    }
}
