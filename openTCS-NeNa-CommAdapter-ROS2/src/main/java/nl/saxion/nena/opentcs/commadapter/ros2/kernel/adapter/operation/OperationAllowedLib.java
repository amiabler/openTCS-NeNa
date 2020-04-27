package nl.saxion.nena.opentcs.commadapter.ros2.kernel.adapter.operation;

import nl.saxion.nena.opentcs.commadapter.ros2.kernel.adapter.Ros2CommAdapter;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.adapter.operation.constants.LoadState;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.adapter.operation.constants.OperationConflictConstants;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.adapter.operation.constants.OperationConstants;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.LoadHandlingDevice;
import org.opentcs.util.ExplainedBoolean;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * Library for meant validating incoming operations.
 */
public class OperationAllowedLib {
    private static LoadState lastKnownLoadState = LoadState.EMPTY;

    public static void setLastKnownLoadState(@Nonnull List<LoadHandlingDevice> loadHandlingDevices) {
        if (!loadHandlingDevices.isEmpty() && loadHandlingDevices.get(0).isFull()) {
            lastKnownLoadState = LoadState.FULL;
        } else {
            lastKnownLoadState = LoadState.EMPTY;
        }
    }

    /**
     * Check if a list of operations is allowed.
     *
     * @param operations      The operation list.
     * @param adapterInstance An instance of the current adapter.
     * @return Whether all operations in the list are allowed.
     */
    public static ExplainedBoolean areAllOperationsAllowed(
            @Nonnull List<String> operations,
            @Nonnull Ros2CommAdapter adapterInstance
    ) {
        ExplainedBoolean isLastOperationAllowed = allowed();

        Iterator<String> operationIterator = operations.iterator();
        while (operationIterator.hasNext() && isLastOperationAllowed.getValue()) {
            final String nextOperation = operationIterator.next();

            isLastOperationAllowed = isOperationAllowed(nextOperation, adapterInstance);
        }

        // All operations are checked, thus allowed.
        return isLastOperationAllowed;
    }

    /**
     * Check if a single operation is allowed.
     *
     * @param operation the operation.
     * @param adapterInstance the adapter instance.
     * @return Whether the operation is allowed.
     */
    public static ExplainedBoolean isOperationAllowed(@Nonnull String operation, Ros2CommAdapter adapterInstance) {
        switch (operation) {
            case OperationConstants.MOVE:
                return isMoveAllowed(adapterInstance);
            case OperationConstants.NOP:
                return isNopAllowed(adapterInstance);
            case OperationConstants.LOAD_CARGO:
                return isLoadCargoAllowed();
            case OperationConstants.UNLOAD_CARGO:
                return isUnloadCargoAllowed();
            default:
                return getUnkownOperation();
        }
    }

    private static ExplainedBoolean isLoadCargoAllowed() {
        boolean isVehicleLoaded = isVehicleLoadedByLoadState(lastKnownLoadState);

        if (isVehicleLoaded) {
            // Vehicle already loaded
            return notAllowed(OperationConflictConstants.LOAD_OPERATION_CONFLICT);
        } else {
            // Allowed
            lastKnownLoadState = LoadState.FULL; // After loading, our vehicle will be FULL.
            return allowed();
        }
    }

    private static ExplainedBoolean isUnloadCargoAllowed() {
        boolean isVehicleLoaded = isVehicleLoadedByLoadState(lastKnownLoadState);

        if (isVehicleLoaded) {
            // Allowed
            return allowed();
        } else {
            // Vehicle already unloaded
            lastKnownLoadState = LoadState.EMPTY; // After unloading, our vehicle will be EMPTY.
            return notAllowed(OperationConflictConstants.UNLOAD_OPERATION_CONFLICT);
        }
    }

    private static ExplainedBoolean isMoveAllowed(@Nonnull Ros2CommAdapter adapterInstance) {
        Vehicle.State currentVehicleState = adapterInstance.getProcessModel().getVehicleState();

        if (currentVehicleState.equals(Vehicle.State.IDLE)) {
            return new ExplainedBoolean(true, "");
        } else {
            return new ExplainedBoolean(false, "Vehicle is not in idle state");
        }
    }

    private static ExplainedBoolean isNopAllowed(@Nonnull Ros2CommAdapter adapterInstance) {
        return isMoveAllowed(adapterInstance);
    }

    private static ExplainedBoolean getUnkownOperation() {
        return new ExplainedBoolean(false, "unknown operation");
    }

    private static boolean isVehicleLoadedByLoadState(LoadState loadState) {

        return loadState == LoadState.FULL;
    }

    private static ExplainedBoolean allowed() {
        return new ExplainedBoolean(true, "");
    }

    private static ExplainedBoolean notAllowed(String reason) {
        return new ExplainedBoolean(false, reason);
    }
}
