package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LootFilterFilenameTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    final LootFilterFileIconsTest.ManualExecutor worker = new LootFilterFileIconsTest.ManualExecutor();
    final AtomicLong clock = new AtomicLong();

    @Test public void fixedDirectoryDoesNotCreateMoveOrClaimAnyFiles() throws Exception
    {
        Path root = temporary.getRoot().toPath();
        Filepath directory = LootFilterFileIcons.iconDirectory(root);
        assertEquals(root.resolve("loot-filters/icons").toString(), directory.toString());
        assertTrue(directory.isRoot());
        try { directory.getParent(); fail("Must be rooted to icons"); }
        catch (IllegalArgumentException expected) { }
        assertFalse(Files.exists(root.resolve("loot-filters")));
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, LootFilterFileIcons::readImage, directory);
        assertNull(cache.get("missing.png")); worker.run(); assertNull(cache.get("missing.png"));
        assertFalse(Files.exists(root.resolve("loot-filters")));
        assertFalse(Files.exists(root.resolve("plugin-data")));

        directory.createDirectories(); // User/plugin setup simulated only in this fixture.
        LootFilterFileIconsTest.write(directory.join("missing.png"), Color.CYAN);
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS);
        cache.get("missing.png"); worker.run();
        assertEquals(Color.CYAN.getRGB(), cache.get("missing.png").getRGB(8, 7));
        assertTrue(directory.join("missing.png").isFile());
        assertFalse(Files.exists(root.resolve("plugin-data")));
    }

    @Test public void nestedNamesSpacesAndUnicodeResolveAndShareCacheAcrossSeparators() throws Exception
    {
        Filepath directory = LootFilterFileIcons.iconDirectory(temporary.getRoot().toPath());
        directory.join("pack icons").createDirectories();
        Filepath icon = directory.join("pack icons/rare é.png");
        LootFilterFileIconsTest.write(icon, Color.GREEN);
        AtomicInteger reads = new AtomicInteger();
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, p -> {
            assertEquals(icon, p); reads.incrementAndGet(); return LootFilterFileIcons.readImage(p);
        }, directory);
        assertNull(cache.get("pack icons/rare é.png"));
        assertNull(cache.get("pack icons\\rare é.png"));
        assertEquals(1, worker.tasks.size()); assertEquals(0, reads.get());
        worker.run();
        BufferedImage first = cache.get("pack icons/rare é.png");
        assertEquals(Color.GREEN.getRGB(), first.getRGB(8, 7));
        assertSame(first, cache.get("pack icons\\rare é.png"));
        assertEquals(1, reads.get());
    }

    @Test public void invalidNamesCannotScheduleReadsOutsideTheFixedDirectory()
    {
        Filepath directory = LootFilterFileIcons.iconDirectory(temporary.getRoot().toPath());
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, p -> {
            fail("Invalid filename reached reader: " + p); return null;
        }, directory);
        String[] invalid = {null, "", " ", "/outside.png", "\\outside.png", "../outside.png", "..\\outside.png",
            "pack/../outside.png", "./icon.png", "pack/./icon.png", "C:/outside.png", "C:outside.png",
            "\\\\server\\share\\icon.png", "https://example.com/icon.png", "file:///outside.png",
            "a//b.png", "a\\\\b.png", "icon.png:stream", "NUL.png", "CON", "COM1.png", "LPT1",
            "bad\u0000.png", "bad\n.png", "icon.png.", "icon.png ", "~alias/icon.png", "a".repeat(241)};
        for (String filename : invalid) assertNull(String.valueOf(filename), cache.get(filename));
        assertTrue(worker.tasks.isEmpty());
    }

    @Test public void relativeNamesDoNotSelectAnotherRuneliteHome() throws Exception
    {
        Path accountOne = temporary.newFolder("account one").toPath();
        Path accountTwo = temporary.newFolder("account two").toPath();
        Filepath first = LootFilterFileIcons.iconDirectory(accountOne), second = LootFilterFileIcons.iconDirectory(accountTwo);
        first.createDirectories(); second.createDirectories();
        LootFilterFileIconsTest.write(first.join("icon.png"), Color.RED);
        LootFilterFileIconsTest.write(second.join("icon.png"), Color.BLUE);
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, LootFilterFileIcons::readImage, first);
        cache.get("icon.png"); worker.run();
        assertEquals(Color.RED.getRGB(), cache.get("icon.png").getRGB(8, 7));
        assertNull(cache.get(second.join("icon.png").toString()));
        assertTrue(worker.tasks.isEmpty());
    }

    @Test public void selectedDirectoryAlsoWorksWithoutTheLegacyDirectoryHelper() throws Exception
    {
        // This is the alternative root supplied by Filepath.Chooser if manual review requires a picker.
        Filepath selected = Filepath.Unchecked.getRooted(temporary.newFolder("chosen icons").toPath());
        LootFilterFileIconsTest.write(selected.join("icon.png"), Color.YELLOW);
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, LootFilterFileIcons::readImage, selected);
        cache.get("icon.png"); worker.run();
        assertEquals(Color.YELLOW.getRGB(), cache.get("icon.png").getRGB(8, 7));
    }
}
