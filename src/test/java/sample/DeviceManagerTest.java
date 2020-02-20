package sample;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DeviceManagerTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyToRegistrationRequests() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device", probe.getRef()));
        DeviceManager.DeviceRegistered registered1 = probe.receiveMessage();

        //another deviceId
        managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device3", probe.getRef()));
        DeviceManager.DeviceRegistered registered2 = probe.receiveMessage();
        assertNotEquals(registered1.device, registered2.device);

        //Check that the device actors are working
        TestProbe<Device.TemperatureRecorded> recordProbe = testKit.createTestProbe(Device.TemperatureRecorded.class);
        registered1.device.tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
        assertEquals(0L, recordProbe.receiveMessage().requestId);
        registered2.device.tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId);
    }

    @Test
    public void testReturnEmptyListForWrongGroupId() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device", probe.getRef()));
        TestProbe<DeviceManager.ReplyDeviceList> probe2 = testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);
        managerActor.tell(new DeviceManager.RequestDeviceList(0L, "group2", probe2.getRef()));
        DeviceManager.ReplyDeviceList replyDeviceList = probe2.receiveMessage();
        assertEquals(Collections.emptySet(), replyDeviceList.ids);
    }

    @Test
    public void testReturnDifferentDeviceActorForDifferentGroupId() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device1", probe.getRef()));

        managerActor.tell(new DeviceManager.RequestTrackDevice("group2", "device2", probe.getRef()));

        TestProbe<DeviceManager.ReplyDeviceList> probe2 = testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);
        managerActor.tell(new DeviceManager.RequestDeviceList(0L, "group", probe2.getRef()));

        DeviceManager.ReplyDeviceList replyDeviceList = probe2.receiveMessage();
        assertEquals(1, replyDeviceList.ids.size());
    }

    @Test
    public void testListActiveDevices() {
        TestProbe<DeviceManager.DeviceRegistered> registeredProbe =
                testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(
                new DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        managerActor.tell(
                new DeviceManager.RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        TestProbe<DeviceManager.ReplyDeviceList> deviceListProbe =
                testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);

        managerActor.tell(new DeviceManager.RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        DeviceManager.ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
    }
}
