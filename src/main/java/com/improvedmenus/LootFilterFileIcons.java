package com.improvedmenus;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Filepath;

/** Bounded, read-only cache for file icons selected by the local filter evaluator. */
@Singleton
final class LootFilterFileIcons
{
    static final int MAX_ENTRIES = 256;
    static final int MAX_PENDING = 32;
    static final int MAX_BYTES = 2 * 1024 * 1024;
    static final long REFRESH_NANOS = TimeUnit.SECONDS.toNanos(5);

    interface Reader { BufferedImage read(Filepath path) throws IOException; }

    private final Executor executor;
    private final LongSupplier clock;
    private final Reader reader;
    private final Filepath directory;
    private final Map<Filepath, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final ArrayDeque<Entry> pending = new ArrayDeque<>();
    private boolean draining;

    private static final class Entry
    {
        final Filepath path;
        BufferedImage image;
        long retryAt;
        boolean loading;
        Entry(Filepath path) { this.path = path; }
    }

    @Inject LootFilterFileIcons(ScheduledExecutorService executor)
    {
        this(executor, System::nanoTime, LootFilterFileIcons::readImage,
            iconDirectory(RuneLite.RUNELITE_DIR.toPath()));
    }

    LootFilterFileIcons(Executor executor, LongSupplier clock, Reader reader)
    {
        this(executor, clock, reader, null);
    }

    LootFilterFileIcons(Executor executor, LongSupplier clock, Reader reader, Filepath directory)
    {
        this.executor = executor;
        this.clock = clock;
        this.reader = reader;
        this.directory = directory == null ? null : directory.rooted();
    }

    /** Fixed foreign-plugin directory; Filepath.Unchecked requires explicit Hub review before release. */
    static Filepath iconDirectory(Path runeLiteDirectory)
    {
        // Do not claim/migrate Loot Filters' directory through our PluginDescriptor.
        // This constructs a path only; it never creates, moves or modifies their files.
        return Filepath.Unchecked.getLegacyPluginDirectory(runeLiteDirectory, "loot-filters")
            .joinSegment("icons").rooted();
    }

    BufferedImage get(String filename)
    {
        if (directory == null) return null;
        try
        {
            Filepath file = directory;
            for (String segment : IconImages.safeName(filename).split("/")) file = file.joinSegment(segment);
            return get(file);
        }
        catch (IllegalArgumentException ex)
        {
            // Absolute paths, parent traversal and invalid platform filenames never reach the reader.
            return null;
        }
    }

    /** Cache lookup and bounded scheduling only: never opens or stats a file on the caller. */
    synchronized BufferedImage get(Filepath path)
    {
        Entry entry = entries.get(path);
        long now = clock.getAsLong();
        if (entry != null && (entry.loading || now - entry.retryAt < 0)) return entry.image;
        if (pending.size() >= MAX_PENDING) return entry == null ? null : entry.image;
        if (entry == null)
        {
            if (entries.size() >= MAX_ENTRIES)
            {
                Iterator<Entry> oldest = entries.values().iterator();
                boolean removed = false;
                while (oldest.hasNext())
                {
                    if (!oldest.next().loading) { oldest.remove(); removed = true; break; }
                }
                if (!removed) return null;
            }
            entry = new Entry(path.rooted());
            entries.put(entry.path, entry);
        }
        entry.loading = true;
        pending.addLast(entry);
        if (!draining)
        {
            draining = true;
            try { executor.execute(this::drain); }
            catch (RejectedExecutionException ex)
            {
                draining = false;
                for (Entry queued : pending)
                {
                    queued.loading = false;
                    queued.retryAt = now + REFRESH_NANOS;
                }
                pending.clear();
            }
        }
        return entry.image;
    }

    private void drain()
    {
        while (true)
        {
            Entry entry;
            synchronized (this)
            {
                entry = pending.pollFirst();
                if (entry == null) { draining = false; return; }
                if (entries.get(entry.path) != entry) continue;
            }
            BufferedImage image = null;
            try { image = reader.read(entry.path); }
            catch (IOException | RuntimeException ignored)
            {
                // Missing, unreadable, invalid and oversized files are retried on a later menu.
            }
            synchronized (this)
            {
                if (entries.get(entry.path) == entry)
                {
                    entry.image = image;
                    entry.loading = false;
                    entry.retryAt = clock.getAsLong() + REFRESH_NANOS;
                }
            }
        }
    }

    synchronized void clear()
    {
        entries.clear();
        pending.clear();
        // An in-flight read may finish, but entry identity prevents it repopulating this cache.
        // The injected executor belongs to RuneLite; do not stop it or interrupt its tasks.
    }

    synchronized String problem()
    {
        int failed = 0;
        for (Entry entry : entries.values())
            if (!entry.loading && entry.image == null && entry.retryAt != 0) failed++;
        return failed == 0 ? "" : failed + " selected icon file(s) could not load. Check the Loot Filters icons folder.";
    }

    static BufferedImage readImage(Filepath path) throws IOException
    {
        try (InputStream in = path.openInputStream())
        {
            byte[] bytes = in.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Icon exceeds 2 MiB");
            return IconImages.normalize(bytes);
        }
    }
}
