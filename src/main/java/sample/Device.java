package sample;

import akka.actor.typed.ActorRef;

import java.util.Optional;

public class Device {

    public interface  Command{}

    public static final class ReadTemperature implements Command {
        final ActorRef<RespondTemperature> replyTo;

        public ReadTemperature(ActorRef<RespondTemperature> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class RespondTemperature {
        final Optional<Double> value;

        public RespondTemperature(Optional<Double> value) {
            this.value = value;
        }
    }
}
