package codeescape.core.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimationTimingTest {

    @Test
    void smoothStepClampsAndEases() {
        assertEquals(0.0, AnimationTiming.smoothStep(0.0), 1e-9);
        assertEquals(1.0, AnimationTiming.smoothStep(1.0), 1e-9);
        assertEquals(0.0, AnimationTiming.smoothStep(-1.0), 1e-9);
        assertEquals(1.0, AnimationTiming.smoothStep(2.0), 1e-9);
        double mid = AnimationTiming.smoothStep(0.5);
        assertEquals(0.5, mid, 1e-9);
        assertTrue(AnimationTiming.smoothStep(0.25) < AnimationTiming.smoothStep(0.5));
        assertTrue(AnimationTiming.smoothStep(0.5) < AnimationTiming.smoothStep(0.75));
    }

    @Test
    void frameIndexSequencesAndHandlesEdges() {
        assertEquals(0, AnimationTiming.frameIndex(0L, 4));
        assertEquals(0, AnimationTiming.frameIndex(-100L, 4));
        assertEquals(0, AnimationTiming.frameIndex(0L, 0));
        assertEquals(1, AnimationTiming.frameIndex(AnimationTiming.RUN_FRAME_DURATION_NANOS, 4));
        assertEquals(0, AnimationTiming.frameIndex(AnimationTiming.RUN_FRAME_DURATION_NANOS * 4L, 4));
        assertTrue(AnimationTiming.RUN_DURATION_NANOS > 0);
        assertTrue(AnimationTiming.RUN_FRAME_DURATION_NANOS > 0);
    }
}
