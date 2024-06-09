package net.xavil.hawklib.client.gl;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.ErrorRatelimiter;
import net.xavil.hawklib.collections.impl.EnumMap;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.ultraviolet.Mod;

public final class GlPerf {

    private GlPerf() {
    }

    private static final Vector<GpuPerformanceTimer> TIMERS = new Vector<>();
    public static final EnumMap<GlObject.ObjectType, GlObjectMetrics> OBJECT_METRICS = new EnumMap<GlObject.ObjectType, GlObjectMetrics>(
            GlObject.ObjectType.class).populate(type -> new GlObjectMetrics(128));
    public static final GlObjectMetrics ROOT_OBJECT_METRICS = new GlObjectMetrics(1024);

    public static final class GlObjectMetrics {
        private int totalObjects = 0;
        private final int[] creationHistory, deletionHistory;
        private final int historySize;
        private int currentHistoryIndex = 0;

        private GlObjectMetrics(int historySize) {
            this.historySize = historySize;
            this.creationHistory = new int[historySize];
            this.deletionHistory = new int[historySize];
        }

        public int totalObjects() {
            return this.totalObjects;
        }

        private void objectCreated() {
            this.creationHistory[this.currentHistoryIndex] += 1;
            this.totalObjects += 1;
        }

        private void objectDestroyed() {
            this.deletionHistory[this.currentHistoryIndex] += 1;
            this.totalObjects -= 1;
        }

        private void tick() {
            this.currentHistoryIndex = (this.currentHistoryIndex + 1) % this.historySize;
            this.creationHistory[this.currentHistoryIndex] = 0;
            this.deletionHistory[this.currentHistoryIndex] = 0;
        }
    }

    public static void objectCreated(GlObject.ObjectType type) {
        OBJECT_METRICS.getOrThrow(type).objectCreated();
        ROOT_OBJECT_METRICS.objectCreated();
    }

    public static void objectDestroyed(GlObject.ObjectType type) {
        OBJECT_METRICS.getOrThrow(type).objectDestroyed();
        ROOT_OBJECT_METRICS.objectDestroyed();
    }

    public static void framebufferChanged() {
    }

    private static int depth = 0;

    public static void beforeRender() {
        if (depth++ != 0) {
            push("forced_tick");
            return;
        }
        ROOT.framesSinceLastUsed = 0;
        ROOT.timer.begin();
    }

    public static void afterRender() {
        if (--depth != 0) {
            pop();
            return;
        }

        OBJECT_METRICS.values().forEach(GlObjectMetrics::tick);
        ROOT_OBJECT_METRICS.tick();

        if (CURRENT != ROOT) {
            while (CURRENT != ROOT) {
                // make sure we end all the timers in the error case so we don't accumulate tons
                // of query objects that will never be set!
                CURRENT.timer.end();
                CURRENT = CURRENT.parent;
            }
            if (!UNBALANCED_RATELIMITER.throttle()) {
                Mod.LOGGER.error("unbalanced GPU scope tree: push() did not correspond with a pop()");
            }
        }

        ROOT.timer.end();
        ROOT.tick();
    }

    public static GpuPerformanceTimer createTimer(String name) {
        final var timer = new GpuPerformanceTimer(name, 16);
        TIMERS.push(timer);
        return timer;
    }

    public static final class TimerTreeNode implements Disposable {
        public static final int HISTORY_SIZE = 16;

        private final TimerTreeNode parent;
        private final Vector<TimerTreeNode> children = new Vector<>();
        public final GpuPerformanceTimer timer;
        private int framesSinceLastUsed;

        private TimerTreeNode(TimerTreeNode parent, String name) {
            this.parent = parent;
            this.timer = new GpuPerformanceTimer(name, HISTORY_SIZE);
        }

        public ImmutableList<TimerTreeNode> children() {
            return ImmutableList.copyOf(this.children);
        }

        public float removalPercent() {
            return this.framesSinceLastUsed / (float) HISTORY_SIZE;
        }

        private boolean shouldRemove() {
            return this.framesSinceLastUsed > HISTORY_SIZE;
        }

        private TimerTreeNode push(String name) {
            final var existingIndex = this.children.indexOf(child -> Objects.equals(child.timer.name, name));
            if (existingIndex < 0) {
                final var node = new TimerTreeNode(this, name);
                this.children.push(node);
                return node;
            } else {
                final var node = this.children.get(existingIndex);
                node.framesSinceLastUsed = 0;
                return node;
            }
        }

        private void tick() {
            this.children.forEach(TimerTreeNode::tick);

            // remove old children
            for (int i = 0; i < this.children.size();) {
                final var child = this.children.get(i);
                if (child.shouldRemove()) {
                    this.children.remove(i).close();
                } else {
                    i += 1;
                }
            }

            this.framesSinceLastUsed += 1;
        }

        @Override
        public void close() {
            this.children.forEach(TimerTreeNode::close);
            this.timer.close();
        }
    }

    public static final TimerTreeNode ROOT = new TimerTreeNode(null, "root");
    private static TimerTreeNode CURRENT = ROOT;

    private static final ErrorRatelimiter UNBALANCED_RATELIMITER = new ErrorRatelimiter(Duration.ofSeconds(10), 1);

    public static void push(String name) {
        CURRENT = CURRENT.push(name);
        CURRENT.timer.begin();
    }

    public static void swap(String name) {
        pop();
        push(name);
    }

    public static void pop() {
        CURRENT.timer.end();
        CURRENT = CURRENT.parent;
        if (CURRENT == null) {
            CURRENT = ROOT;
            if (!UNBALANCED_RATELIMITER.throttle()) {
                Mod.LOGGER.error("unbalanced GPU scope tree: pop() did not correspond with a push()");
            }
        }
    }

    public static Iterator<GpuPerformanceTimer> iterTimers() {
        return TIMERS.iter();
    }

    public static void tick() {
        TIMERS.forEach(GpuPerformanceTimer::tick);
    }

    private static class QueryPair implements Disposable {
        public final GlQuery begin, end;
        public Instant cpuBegin, cpuEnd;

        public QueryPair(GlQuery begin, GlQuery end) {
            this.begin = begin;
            this.end = end;
        }

        public QueryPair() {
            this(new GlQuery(GlQuery.Type.TIMESTAMP), new GlQuery(GlQuery.Type.TIMESTAMP));
        }

        public boolean isResultAvailable() {
            return this.begin.isResultAvailable() && this.end.isResultAvailable();
        }

        public long getGpuDurationNs() {
            return this.end.clientWaitValue() - this.begin.clientWaitValue();
        }

        public long getCpuDurationNs() {
            return Duration.between(this.cpuBegin, this.cpuEnd).toNanos();
        }

        @Override
        public void close() {
            if (this.begin != null)
                this.begin.close();
            if (this.end != null)
                this.end.close();
        }
    }

    public static class GpuPerformanceTimer implements Disposable {
        private final Vector<QueryPair> inFlightQueries = new Vector<>();
        private final Vector<QueryPair> freeQueries = new Vector<>();
        private QueryPair currentQuery;

        public final String name;
        private final int historySize;
        private float[] durationHistoryGpu;
        private float[] durationHistoryCpu;
        private int currentHistoryIndex;

        private GpuPerformanceTimer(String name, int historySize) {
            this.name = name;
            this.historySize = historySize;
            this.durationHistoryGpu = new float[historySize];
            this.durationHistoryCpu = new float[historySize];
        }

        private void tick() {
        }

        private QueryPair getQuery() {
            if (!this.freeQueries.isEmpty())
                return this.freeQueries.popOrThrow();
            return new QueryPair();
        }

        public void begin() {
            if (this.currentQuery != null)
                throw new IllegalStateException(String.format(
                        "Cannot begin GPU timer '%s' scope, it is already in use!",
                        this.name));
            this.currentQuery = getQuery();
            this.currentQuery.cpuBegin = Instant.now();
            this.currentQuery.begin.writeTimestamp();
        }

        public void end() {
            if (this.currentQuery == null)
                throw new IllegalStateException(String.format(
                        "Cannot end GPU timer '%s' scope, it was never begun!",
                        this.name));

            this.currentQuery.cpuEnd = Instant.now();
            this.currentQuery.end.writeTimestamp();
            this.inFlightQueries.push(this.currentQuery);
            this.currentQuery = null;

            for (int i = 0; i < this.inFlightQueries.size(); ++i) {
                final var query = this.inFlightQueries.get(i);
                if (query.isResultAvailable()) {
                    this.inFlightQueries.set(i, null);
                    this.freeQueries.push(query);
                    // result is available, we dont actually have to wait.
                    this.durationHistoryGpu[this.currentHistoryIndex] = query.getGpuDurationNs() / 1e9f;
                    this.durationHistoryCpu[this.currentHistoryIndex] = query.getCpuDurationNs() / 1e9f;
                    this.currentHistoryIndex = (this.currentHistoryIndex + 1) % this.historySize;
                }
            }

            this.inFlightQueries.retain(value -> value != null);
        }

        public float windowedAverageGpuTime() {
            float sum = 0f;
            for (int i = 0; i < this.durationHistoryGpu.length; ++i)
                sum += this.durationHistoryGpu[i];
            return sum / this.durationHistoryGpu.length;
        }

        public float windowedAverageCpuTime() {
            float sum = 0f;
            for (int i = 0; i < this.durationHistoryCpu.length; ++i)
                sum += this.durationHistoryCpu[i];
            return sum / this.durationHistoryCpu.length;
        }

        @Override
        public void close() {
            if (this.currentQuery != null)
                this.currentQuery.close();
            this.freeQueries.forEach(QueryPair::close);
            this.inFlightQueries.forEach(QueryPair::close);
            this.freeQueries.clear();
            this.inFlightQueries.clear();
        }
    }

}
