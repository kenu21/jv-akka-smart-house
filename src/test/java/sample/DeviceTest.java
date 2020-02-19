package sample;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DeviceTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestProbe<Device.RespondTemperature> probe = testKit.createTestProbe(Device.RespondTemperature.class);
        ActorRef<Device.Command> deviceActor = testKit.spawn(Device.create("group", "device"));
        deviceActor.tell(new Device.ReadTemperature(42L, probe.getRef()));
        Device.RespondTemperature response = probe.receiveMessage();
        assertEquals(42L, response.requestId);
        assertEquals(Optional.empty(), response.value);
    }
}
