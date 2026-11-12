package codeescape.core.animation;

/**
 * Platform-agnostic runner transition state. Frontends render using these values
 * and apply viewport scaling; timing/easing/frame sequencing remain identical.
 */
public class RunnerAnimationState {
    public final long durationNanos;
    public final int frameCount;
    private long startNanos = -1;

    public RunnerAnimationState(int frameCount) {
        this(AnimationTiming.RUN_DURATION_NANOS, frameCount);
    }

    public RunnerAnimationState(long durationNanos, int frameCount) {
        this.durationNanos = durationNanos <= 0 ? AnimationTiming.RUN_DURATION_NANOS : durationNanos;
        this.frameCount = Math.max(1, frameCount);
    }

    public void start(long nowNanos) { this.startNanos = nowNanos; }
    public boolean isStarted() { return startNanos >= 0; }

    public long elapsed(long nowNanos) {
        if (startNanos < 0) return 0;
        return Math.max(0L, nowNanos - startNanos);
    }

    public double linearProgress(long nowNanos) {
        return Math.min(1.0, elapsed(nowNanos) / (double) durationNanos);
    }

    public double easedProgress(long nowNanos) {
        return AnimationTiming.smoothStep(linearProgress(nowNanos));
    }

    public int frame(long nowNanos) {
        return AnimationTiming.frameIndex(elapsed(nowNanos), frameCount);
    }

    public boolean finished(long nowNanos) {
        return linearProgress(nowNanos) >= 1.0;
    }

    /**
     * Compute x position in pixels for a runner of given display width moving across available width.
     */
    public double x(long nowNanos, double viewWidth, double spriteDisplayWidth) {
        double target = Math.max(10.0, viewWidth - spriteDisplayWidth - 20.0);
        double startX = -spriteDisplayWidth - 4.0;
        return startX + (target - startX) * easedProgress(nowNanos);
    }
}
