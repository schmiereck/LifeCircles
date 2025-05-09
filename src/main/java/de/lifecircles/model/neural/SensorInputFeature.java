package de.lifecircles.model.neural;

/**
 * Enum defining feature indices for each SensorActor input block.
 */
public enum SensorInputFeature {
    MY_ACTOR_RED,
    MY_ACTOR_GREEN,
    MY_ACTOR_BLUE,
    MY_ACTOR_FORCE_STRENGTH,
    MY_ACTOR_TOP_POSITION,

    SENSED_CELL_TYPE_RED,
    SENSED_CELL_TYPE_GREEN,
    SENSED_CELL_TYPE_BLUE,
    SENSED_CELL_ENERGY,
    SENSED_CELL_AGE,

    SENSED_ACTOR_FORCE_STRENGTH,
    SENSED_ACTOR_TYPE_RED,
    SENSED_ACTOR_TYPE_GREEN,
    SENSED_ACTOR_TYPE_BLUE,
    SENSED_CELL_STATE_0,
    SENSED_CELL_STATE_1,
    SENSED_CELL_STATE_2
}
