package com.feel.gems.util;

import com.feel.gems.GemsMod;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Central server-thread tick scheduler (tick-time based).
 *
 * <p>Use this to avoid registering lots of independent tick listeners. Tasks run on the server
 * thread during {@link ServerTickEvents#END_SERVER_TICK}.</p>
 */
public final class GemsTickScheduler {
    private static final PriorityQueue<Task> QUEUE = new PriorityQueue<>(
            Comparator.<Task>comparingLong(t -> t.nextTick)
                    .thenComparingInt(t -> t.priority)
                    .thenComparingLong(t -> t.id)
    );
    private static final Map<Long, Task> TASKS = new HashMap<>();
    private static long nextId = 1L;
    private static boolean registered = false;

    private GemsTickScheduler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(GemsTickScheduler::onEndServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
    }

    public static Handle schedule(MinecraftServer server, int delayTicks, Consumer<MinecraftServer> task) {
        long now = GemsTime.now(server);
        return add(now + Math.max(0, delayTicks), 0, 0, task);
    }

    public static Handle scheduleRepeating(MinecraftServer server, int initialDelayTicks, int periodTicks, Consumer<MinecraftServer> task) {
        long now = GemsTime.now(server);
        int period = Math.max(1, periodTicks);
        return add(now + Math.max(0, initialDelayTicks), period, 0, task);
    }

    public static Handle scheduleRepeating(MinecraftServer server, int initialDelayTicks, int periodTicks, int priority, Consumer<MinecraftServer> task) {
        long now = GemsTime.now(server);
        int period = Math.max(1, periodTicks);
        return add(now + Math.max(0, initialDelayTicks), period, priority, task);
    }

    public static void clearAll() {
        QUEUE.clear();
        TASKS.clear();
    }

    private static Handle add(long nextTick, int periodTicks, int priority, Consumer<MinecraftServer> task) {
        long id = nextId++;
        Task entry = new Task(id, nextTick, periodTicks, priority, task);
        TASKS.put(id, entry);
        QUEUE.add(entry);
        return new Handle(id);
    }

    private static void onEndServerTick(MinecraftServer server) {
        long now = GemsTime.now(server);

        while (true) {
            Task head = QUEUE.peek();
            if (head == null || head.nextTick > now) {
                return;
            }
            QUEUE.poll();
            if (head.cancelled || TASKS.get(head.id) != head) {
                continue;
            }

            try {
                head.task.accept(server);
            } catch (Throwable t) {
                GemsMod.LOGGER.error("Scheduled task {} failed", head.id, t);
            }

            if (head.periodTicks > 0 && !head.cancelled) {
                long next = head.nextTick + head.periodTicks;
                if (next <= now) {
                    // Avoid immediate tight loops if the server lags; reschedule to the next future tick.
                    long behind = now - head.nextTick;
                    long steps = (behind / head.periodTicks) + 1;
                    next = head.nextTick + steps * head.periodTicks;
                }
                head.nextTick = next;
                QUEUE.add(head);
            } else {
                TASKS.remove(head.id);
            }
        }
    }

    public static final class Handle {
        private final long id;

        private Handle(long id) {
            this.id = id;
        }

        public void cancel() {
            Task task = TASKS.remove(id);
            if (task != null) {
                task.cancelled = true;
            }
        }
    }

    private static final class Task {
        private final long id;
        private final int periodTicks;
        private final int priority;
        private final Consumer<MinecraftServer> task;
        private long nextTick;
        private boolean cancelled;

        private Task(long id, long nextTick, int periodTicks, int priority, Consumer<MinecraftServer> task) {
            this.id = id;
            this.nextTick = nextTick;
            this.periodTicks = periodTicks;
            this.priority = priority;
            this.task = task;
        }
    }
}
