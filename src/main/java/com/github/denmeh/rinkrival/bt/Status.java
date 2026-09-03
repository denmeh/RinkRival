package com.github.denmeh.rinkrival.bt;

/**
 * What a {@link Node} reports after a tick. Three values only, which is the whole point: anything more
 * and composites stop being composable.
 */
public enum Status {
    /** Still working. Tick me again next tick. */
    RUNNING,
    /** Finished the job. */
    SUCCESS,
    /** Cannot run, or cannot continue. Never "not done yet". */
    FAILURE
}
