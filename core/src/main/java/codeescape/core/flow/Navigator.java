package codeescape.core.flow;

/**
 * UI-implemented navigation contract. Core emits navigation intents; frontends perform
 * screen changes, transitions, and platform-specific effects while preserving parity.
 */
public interface Navigator {
    void navigate(FlowState state);

    /** Optional context for transitions that depend on level (e.g. story/running). */
    default void navigate(FlowState state, int level) {
        navigate(state);
    }
}
