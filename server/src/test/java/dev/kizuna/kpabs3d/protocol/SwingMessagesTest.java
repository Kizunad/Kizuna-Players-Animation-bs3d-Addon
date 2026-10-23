package dev.kizuna.kpabs3d.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.Hello;
import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.State;

import org.junit.jupiter.api.Test;

final class SwingMessagesTest {
    @Test
    void stateRoundTripKeepsAuthoritativeParameters() {
        var settings = SwingMessages.defaults();
        State state = settings.toState(7);

        assertEquals(settings, SwingMessages.validate(state));
        assertEquals(7, state.getRevision());
    }

    @Test
    void invalidStateValuesAreRejected() {
        State invalid = SwingMessages.defaults().toState(1).toBuilder()
                .setAmplitudeDegrees(46)
                .build();

        assertThrows(IllegalArgumentException.class, () -> SwingMessages.validate(invalid));
    }

    @Test
    void helloRequiresTheCurrentProtocolVersion() {
        Hello invalid = Hello.newBuilder().setProtocolVersion(2).build();

        assertThrows(IllegalArgumentException.class, () -> SwingMessages.validate(invalid));
    }
}
