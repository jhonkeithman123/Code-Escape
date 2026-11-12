package codeescape.core.animation;

/**
 * Shared animation timing/constants so desktop and Android renderers stay in sync.
 * Renderers must apply viewport scaling but keep durations, easing, and frame sequencing identical.
 */
public final class AnimationTiming {
    private AnimationTiming() {}

    public static final long RUN_DURATION_NANOS = 6_000_000_000L;
    public static final long RUN_FRAME_DURATION_NANOS = 120_000_000L;

    /** Smoothstep easing in [0,1] for transition runner progress. */
    public static double smoothStep(double t) {
        double p = Math.max(0.0, Math.min(1.0, t));
        return p * p * (3.0 - 2.0 * p);
    }

    public static int frameIndex(long elapsedNanos, int frameCount) {
        if (frameCount <= 0) return 0;
        return (int) ((Math.max(0L, elapsedNanos) / RUN_FRAME_DURATION_NANOS) % frameCount);
    }
}
