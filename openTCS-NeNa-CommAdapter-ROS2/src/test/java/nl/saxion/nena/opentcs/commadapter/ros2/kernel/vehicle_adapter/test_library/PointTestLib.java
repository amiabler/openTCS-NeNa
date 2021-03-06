/**
 * Copyright (c) Niels Tiben (nielstiben@outlook.com)
 */
package nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.test_library;

import org.opentcs.data.model.Point;
import org.opentcs.data.model.Triple;

import javax.annotation.Nonnull;

/**
 * Library class for generating a dummy point.
 *
 * @author Niels Tiben
 */
public abstract class PointTestLib {
    public static Point generatePointByNameAndCoordinate(@Nonnull String name, @Nonnull Triple coordinate) {
        Point point = new Point(name);

        return point.withPosition(coordinate);
    }
}
