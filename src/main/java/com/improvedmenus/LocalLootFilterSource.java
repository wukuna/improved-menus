package com.improvedmenus;

import com.improvedmenus.rs2f.LootFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Filepath;

/** Reads and compiles data on a worker. Never writes foreign settings/files or downloads scripts. */
@Singleton
final class LocalLootFilterSource
{
    static final int MAX_BYTES = 1000000;
    static final long POLL_NANOS = TimeUnit.SECONDS.toNanos(2);
    static final Map<String, String> DEFAULTS = Map.of(
        "[default: Rikten's filter]", "defaultriktensfilter.rs2f.gz",
        "[default: Joe's filter]", "defaultjoesfilter.rs2f.gz",
        "[default: Storn's filter]", "defaultstornsfilter.rs2f.gz");

    interface Reader { String read(String selected) throws IOException; }
    static final class Loaded
    {
        final LocalFilterSettings settings;
        final LootFilter program;
        final String source;
        Loaded(LocalFilterSettings settings, LootFilter program, String source)
        { this.settings = settings; this.program = program; this.source = source; }
    }

    private final Executor executor;
    private final LongSupplier clock;
    private final Reader reader;
    private LocalFilterSettings requested;
    private Loaded lastSuccessful;
    private volatile Loaded current;
    private volatile String status = "Loot Filters compatibility is off.";
    private volatile long revision;
    private long generation, nextPoll;
    private boolean reading;

    @Inject LocalLootFilterSource(ScheduledExecutorService executor)
    {
        this(executor, System::nanoTime, defaultReader());
    }

    LocalLootFilterSource(Executor executor, LongSupplier clock, Reader reader)
    { this.executor = executor; this.clock = clock; this.reader = reader; }

    private static Reader defaultReader()
    {
        // Read-only use, but obtaining this foreign root is restricted for eventual Hub review.
        Filepath directory = Filepath.Unchecked.getLegacyPluginDirectory(RuneLite.RUNELITE_DIR.toPath(), "loot-filters")
            .joinSegment("filters").rooted();
        return selected -> read(directory, selected);
    }

    static String read(Filepath directory, String selected) throws IOException
    {
        if (selected == null || selected.isEmpty()) return "";
        String resource = DEFAULTS.get(selected);
        if (resource != null)
        {
            InputStream bundled = LocalLootFilterSource.class.getResourceAsStream("/com/improvedmenus/lootfilters-defaults/" + resource);
            if (bundled == null) throw new IOException("Bundled filter is missing");
            try (InputStream in = new GZIPInputStream(bundled)) { return text(in); }
        }
        // A selected filter is a filename in this directory, never a path or URL.
        if (selected.length() > 240 || selected.startsWith("."))
            throw new IllegalArgumentException("Unsupported selected filter filename");
        try (InputStream in = directory.joinSegment(selected).openInputStream()) { return text(in); }
    }

    private static String text(InputStream in) throws IOException
    {
        byte[] data = in.readNBytes(MAX_BYTES + 1);
        if (data.length > MAX_BYTES) throw new IOException("Filter exceeds 1 MB");
        return new String(data, StandardCharsets.UTF_8);
    }

    synchronized void refresh(LocalFilterSettings settings)
    {
        if (!Objects.equals(requested, settings))
        {
            generation++;
            requested = settings;
            current = null;
            revision++;
            nextPoll = 0;
            status = "Reading selected Loot Filters rules...";
        }
        long now = clock.getAsLong();
        if (reading || now - nextPoll < 0) return;
        reading = true;
        long token = generation;
        Loaded previous = lastSuccessful;
        nextPoll = now + POLL_NANOS;
        try { executor.execute(() -> load(settings, token, previous)); }
        catch (RejectedExecutionException ex)
        {
            reading = false;
            current = null;
            status = "Filter reader is unavailable; try toggling compatibility.";
        }
    }

    private void load(LocalFilterSettings settings, long token, Loaded previous)
    {
        Loaded loaded = null;
        String message;
        try
        {
            String source = reader.read(settings.getSelected());
            loaded = previous != null && settings.equals(previous.settings) && source.equals(previous.source)
                ? previous : new Loaded(settings, settings.decorate(LootFilter.compile(source)), source);
            message = "Reading " + (settings.getSelected() == null ? "Loot Filters item lists (no script selected)" : settings.getSelected())
                + ". Icons are evaluated locally; no upload or Loot Filters update needed.";
        }
        catch (IOException | RuntimeException ex)
        {
            message = "Selected filter could not load: " + clean(ex.getMessage())
                + ". Check the selected file in Loot Filters; stale icons are not used.";
        }
        synchronized (this)
        {
            reading = false;
            if (token != generation) return;
            if (current != loaded) revision++;
            current = loaded;
            lastSuccessful = loaded;
            status = message;
        }
    }

    private static String clean(String text)
    {
        if (text == null) return "missing, unreadable or invalid filter";
        return text.substring(0, Math.min(text.length(), 160)).replace('<', '(').replace('>', ')');
    }

    synchronized void clear(String message)
    {
        if (requested == null && current == null && lastSuccessful == null && status.equals(message)) return;
        generation++;
        current = null;
        revision++;
        lastSuccessful = null;
        requested = null;
        nextPoll = 0;
        status = message;
    }

    Loaded current() { return current; }
    String status() { return status; }
    long revision() { return revision; }
}
