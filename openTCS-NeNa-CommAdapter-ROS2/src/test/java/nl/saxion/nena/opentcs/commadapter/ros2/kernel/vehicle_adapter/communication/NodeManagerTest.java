/**
 * Copyright (c) Niels Tiben (nielstiben@outlook.com)
 */
package nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.communication;

import action_msgs.msg.dds.GoalStatusArray;
import geometry_msgs.msg.dds.PoseStamped;
import geometry_msgs.msg.dds.PoseWithCovarianceStamped;
import lombok.SneakyThrows;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.communication.constants.NodeRunningStatus;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.library.OutgoingMessageLib;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.library.ScaleCorrector;
import nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.point.CoordinatePoint;
import org.junit.Before;
import org.junit.Test;
import org.opentcs.data.model.Triple;

import java.util.Calendar;

import static nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.test_library.Ros2CommAdapterTestLib.DEFAULT_TESTING_DOMAIN_ID;
import static nl.saxion.nena.opentcs.commadapter.ros2.kernel.vehicle_adapter.test_library.Ros2CommAdapterTestLib.DEFAULT_TESTING_NAMESPACE;

/**
 * Unit test to cover {@link NodeManager}.
 *
 * @author Niels Tiben
 */
public class NodeManagerTest {
    private NodeRunningStatusListener nodeRunningStatusListener;
    private NodeMessageListener nodeMessageListener;

    //================================================================================
    // Callback variables.
    //================================================================================

    private NodeRunningStatus lastKnownNodeRunningStatus;

    //================================================================================
    // Pre operations
    //================================================================================

    @Before
    public void setUpListeners() {
        this.nodeRunningStatusListener = nodeRunningStatus -> this.lastKnownNodeRunningStatus = nodeRunningStatus;

        // Ignore message callbacks
        this.nodeMessageListener = new NodeMessageListener() {
            @Override
            public void onNewGoalStatusArray(GoalStatusArray goalStatusArray) { }
            @Override
            public void onNewAmclPose(PoseWithCovarianceStamped amclPose) { }
            @Override
            public void onOperationLoadCargoFeedback(String feedback) { }
            @Override
            public void onOperationUnloadCargoFeedback(String feedback) { }
        };
    }

    //================================================================================
    // Tests
    //================================================================================

    @Test
    public void testLifecycle() {
        // 1: Create the node
        NodeManager nodeManager = new NodeManager();
        this.lastKnownNodeRunningStatus = nodeManager.getNodeRunningStatus();
        assert this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.NOT_ACTIVE);

        // 2: Start the node
        nodeManager.start(this.nodeRunningStatusListener, this.nodeMessageListener,DEFAULT_TESTING_DOMAIN_ID, DEFAULT_TESTING_NAMESPACE);
        assert this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.INITIATING);

        // 3: Node (should) have been active.
        assertNodeRunningStatusFromInitiatingToActive();

        // 4: Stop the node
        nodeManager.stop();
        assert this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.NOT_ACTIVE);
    }

    @SneakyThrows
    @Test(expected = NullPointerException.class)
    public void testNodeUsageFailsOnNotActive(){
        // 1: Create the node
        NodeManager nodeManager = new NodeManager();

        // 2: Try publishing a message on a inactive node.
        CoordinatePoint testPoint = new CoordinatePoint(new Triple(0,0,0));
        ScaleCorrector.getInstance().setScale(1);
        PoseStamped testMessage = OutgoingMessageLib.generateScaledNavigationMessageByPoint(testPoint);

        assert nodeManager.getNodeRunningStatus().equals(NodeRunningStatus.NOT_ACTIVE);
        nodeManager.getNode().getGoalPublisher().publish(testMessage);
    }

    //================================================================================
    // Helper methods
    //================================================================================

    @SneakyThrows
    private synchronized void assertNodeRunningStatusFromInitiatingToActive() {
        Calendar timeLimitStamp = Calendar.getInstance();
        timeLimitStamp.add(Calendar.MILLISECOND, 2500); // It may take 2.5 second.

        assert this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.INITIATING);

        while (this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.INITIATING)) {
            // they are still the same, keep waiting.
            assert !timeLimitReached(timeLimitStamp);
        }

        assert this.lastKnownNodeRunningStatus.equals(NodeRunningStatus.ACTIVE);

    }

    private boolean timeLimitReached(Calendar timeLimitStamp) {
        Calendar currentTimestamp = Calendar.getInstance();
        return currentTimestamp.getTime().getTime() > timeLimitStamp.getTime().getTime();
    }
}
