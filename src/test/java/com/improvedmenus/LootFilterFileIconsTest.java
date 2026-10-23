package com.improvedmenus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LootFilterFileIconsTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    final AtomicLong clock = new AtomicLong();
    final ManualExecutor worker = new ManualExecutor();

    static final class ManualExecutor implements Executor
    {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable task) { tasks.add(task); }
        void run() { while (!tasks.isEmpty()) tasks.remove().run(); }
    }

    Filepath file(String name)
    {
        // Generic fixture helper; filename integration separately exercises the fixed legacy directory.
        return Filepath.Unchecked.getRooted(temporary.getRoot().toPath()).join(name);
    }

    static void write(Filepath file, Color color) throws Exception
    {
        BufferedImage image = new BufferedImage(17, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { g.setColor(color); g.fillRect(0, 0, 17, 15); } finally { g.dispose(); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", out));
        file.write(out.toByteArray());
    }

    @Test public void repeatedMenuRequestsLoadOnceAndHaveIndependentPixels() throws Exception
    {
        Filepath path = file("rare loot é.png");
        write(path, Color.MAGENTA);
        byte[] original = Files.readAllBytes(temporary.getRoot().toPath().resolve(path.getFileName()));
        AtomicInteger reads = new AtomicInteger();
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, p -> {
            reads.incrementAndGet(); return LootFilterFileIcons.readImage(p);
        });
        for (int i = 0; i < 1000; i++) assertNull(cache.get(path));
        assertEquals(0, reads.get()); assertEquals(1, worker.tasks.size());
        worker.run();
        BufferedImage result = cache.get(path);
        assertEquals(1, reads.get()); assertEquals(Color.MAGENTA.getRGB(), result.getRGB(8, 7));
        for (int i = 0; i < 1000; i++) assertSame(result, cache.get(path));
        assertTrue(worker.tasks.isEmpty());
        result.setRGB(8, 7, Color.BLUE.getRGB());
        assertArrayEquals(original, Files.readAllBytes(temporary.getRoot().toPath().resolve(path.getFileName())));
    }

    @Test public void missingCorruptAndOversizeFilesRecoverWithoutRetryStorms() throws Exception
    {
        Filepath path = file("later.png");
        AtomicInteger reads = new AtomicInteger();
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, p -> {
            reads.incrementAndGet(); return LootFilterFileIcons.readImage(p);
        });
        assertNull(cache.get(path)); worker.run();
        for (int i = 0; i < 100; i++) assertNull(cache.get(path));
        assertEquals(1, reads.get());
        path.write("not an image");
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS); cache.get(path); worker.run();
        assertNull(cache.get(path)); assertEquals(2, reads.get());
        path.write(new byte[LootFilterFileIcons.MAX_BYTES + 1]);
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS); cache.get(path); worker.run();
        assertNull(cache.get(path)); assertEquals(3, reads.get());
        write(path, Color.CYAN);
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS); cache.get(path); worker.run();
        assertEquals(Color.CYAN.getRGB(), cache.get(path).getRGB(8, 7));
    }

    @Test public void changedAndDeletedFilesAreRecheckedOnLaterMenus() throws Exception
    {
        Filepath path = file("changing.png"); write(path, Color.RED);
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, LootFilterFileIcons::readImage);
        cache.get(path); worker.run(); assertEquals(Color.RED.getRGB(), cache.get(path).getRGB(8, 7));
        write(path, Color.GREEN);
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS);
        cache.get(path); worker.run(); assertEquals(Color.GREEN.getRGB(), cache.get(path).getRGB(8, 7));
        path.delete(); clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS);
        cache.get(path); worker.run(); assertNull(cache.get(path));
    }

    @Test public void imageLimitsTransparencyAndCommonFormatsAreHandled() throws Exception
    {
        for (String format : new String[]{"png", "gif", "jpeg"})
        {
            BufferedImage input = new BufferedImage(32, 16,
                format.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = input.createGraphics();
            try { g.setColor(Color.RED); g.fillRect(8, 4, 16, 8); } finally { g.dispose(); }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(input, format, out));
            Filepath path = file("test." + format); path.write(out.toByteArray());
            BufferedImage result = LootFilterFileIcons.readImage(path);
            assertEquals(17, result.getWidth()); assertEquals(15, result.getHeight());
            assertEquals(0, result.getRGB(0, 0) >>> 24);
            assertTrue((result.getRGB(8, 7) & 0xff0000) > 0xc00000);
        }
        BufferedImage huge = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(huge, "png", out);
        Filepath path = file("oversize-dimensions.png"); path.write(out.toByteArray());
        try { LootFilterFileIcons.readImage(path); fail("Oversize dimensions accepted"); }
        catch (IllegalArgumentException expected) { }
        Filepath rooted = file("single.png").rooted();
        try { rooted.getParent(); fail("File handle escaped its root"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void queueAndCacheStayBoundedAndOverflowCanRetry() throws Exception
    {
        AtomicInteger reads = new AtomicInteger();
        BufferedImage image = new BufferedImage(17, 15, 2);
        LootFilterFileIcons cache = new LootFilterFileIcons(worker, clock::get, p -> { reads.incrementAndGet(); return image; });
        for (int i = 0; i < 1000; i++) cache.get(file(i + ".png"));
        assertEquals(1, worker.tasks.size()); worker.run(); assertEquals(LootFilterFileIcons.MAX_PENDING, reads.get());
        assertNull(cache.get(file("999.png"))); worker.run(); assertSame(image, cache.get(file("999.png")));
        for (int i = 1000; i < 1000 + LootFilterFileIcons.MAX_ENTRIES; i++) { cache.get(file(i + ".png")); worker.run(); }
        assertNull(cache.get(file("0.png"))); worker.run(); assertSame(image, cache.get(file("0.png")));
    }

    @Test public void clearDiscardsQueuedAndInFlightResults() throws Exception
    {
        AtomicInteger reads = new AtomicInteger();
        LootFilterFileIcons[] cache = new LootFilterFileIcons[1];
        cache[0] = new LootFilterFileIcons(worker, clock::get, p -> {
            reads.incrementAndGet(); cache[0].clear(); return new BufferedImage(17, 15, 2);
        });
        Filepath path = file("reset.png"); cache[0].get(path); cache[0].clear(); worker.run();
        assertEquals(0, reads.get());
        cache[0].get(path); worker.run(); assertEquals(1, reads.get());
        assertNull(cache[0].get(path)); // The cleared in-flight result did not reappear.
        assertEquals(1, worker.tasks.size());
    }

    @Test public void realSlowWorkerNeverBlocksTheMenuCaller() throws Exception
    {
        Filepath path = file("worker.png"); write(path, Color.ORANGE);
        ExecutorService background = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        Thread caller = Thread.currentThread();
        LootFilterFileIcons cache = new LootFilterFileIcons(background, clock::get, p -> {
            assertNotSame(caller, Thread.currentThread()); entered.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException ex) { throw new AssertionError(ex); }
            return LootFilterFileIcons.readImage(p);
        });
        try
        {
            assertNull(cache.get(path)); assertTrue(entered.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 1000; i++) assertNull(cache.get(path));
            release.countDown(); background.submit(() -> {}).get(5, TimeUnit.SECONDS);
            assertEquals(Color.ORANGE.getRGB(), cache.get(path).getRGB(8, 7));
        }
        finally { release.countDown(); background.shutdown(); assertTrue(background.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test public void executorRejectionDoesNotBreakMenusOrSpin() throws Exception
    {
        AtomicInteger submits = new AtomicInteger();
        LootFilterFileIcons cache = new LootFilterFileIcons(task -> {
            submits.incrementAndGet(); throw new RejectedExecutionException();
        }, clock::get, p -> { fail("Must not read on caller"); return null; });
        Filepath path = file("rejected.png");
        for (int i = 0; i < 1000; i++) assertNull(cache.get(path));
        assertEquals(1, submits.get());
        clock.addAndGet(LootFilterFileIcons.REFRESH_NANOS); cache.get(path); assertEquals(2, submits.get());
    }
}
