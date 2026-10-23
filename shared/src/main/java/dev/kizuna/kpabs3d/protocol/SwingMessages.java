package dev.kizuna.kpabs3d.protocol;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.Hello;
import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.State;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** 摆动附属的唯一网络边界；Fabric 载荷只承载 protobuf 字节。 */
public final class SwingMessages {
    public static final String MOD_ID = "kpa_bs3d_swing";
    public static final String MODEL_ID = MOD_ID + ":swing_pouch";
    public static final Identifier HELLO = new Identifier(MOD_ID, "hello");
    public static final Identifier STATE = new Identifier(MOD_ID, "state");
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_BYTES = 8 * 1024;

    private SwingMessages() {
    }

    public static PacketByteBuf write(MessageLite message) {
        // 先检查序列化大小，再创建网络缓冲，避免异常消息制造不受控分配。
        if (message.getSerializedSize() > MAX_BYTES) {
            throw new IllegalArgumentException("摆动协议载荷超过 8 KiB");
        }
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBytes(message.toByteArray());
        return buffer;
    }

    public static <T extends MessageLite> T read(PacketByteBuf buffer, Parser<T> parser)
            throws InvalidProtocolBufferException {
        // Fabric 包没有额外长度字段，整个可读区就是一条 protobuf 消息。
        int size = buffer.readableBytes();
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("摆动协议载荷超过 8 KiB");
        }
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        return parser.parseFrom(bytes);
    }

    public static SwingSettings defaults() {
        return new SwingSettings(true, 12.0f, 1.7f, 0.82f);
    }

    public static SwingSettings disabled() {
        return new SwingSettings(false, 0.0f, 1.7f, 0.82f);
    }

    public static void validate(Hello hello) {
        if (!hello.getUnknownFields().asMap().isEmpty()) {
            throw new IllegalArgumentException("摆动握手含未知字段");
        }
        if (hello.getProtocolVersion() != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("不支持的摆动协议版本：" + hello.getProtocolVersion());
        }
    }

    public static SwingSettings validate(State state) {
        if (!state.getUnknownFields().asMap().isEmpty()) {
            throw new IllegalArgumentException("摆动状态含未知字段");
        }
        if (state.getProtocolVersion() != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("不支持的摆动协议版本：" + state.getProtocolVersion());
        }
        float amplitude = state.getAmplitudeDegrees();
        float frequency = state.getFrequencyHz();
        float damping = state.getDamping();
        if (!Float.isFinite(amplitude) || amplitude < 0 || amplitude > 45) {
            throw new IllegalArgumentException("摆动幅度必须在 [0,45] 度");
        }
        if (!Float.isFinite(frequency) || frequency <= 0 || frequency > 8) {
            throw new IllegalArgumentException("摆动频率必须在 (0,8] Hz");
        }
        if (!Float.isFinite(damping) || damping < 0 || damping > 1) {
            throw new IllegalArgumentException("摆动阻尼必须在 [0,1]");
        }
        return new SwingSettings(state.getEnabled(), amplitude, frequency, damping);
    }

    public record SwingSettings(boolean enabled, float amplitudeDegrees, float frequencyHz,
                                float damping) {
        public SwingSettings {
            if (!Float.isFinite(amplitudeDegrees) || amplitudeDegrees < 0 || amplitudeDegrees > 45) {
                throw new IllegalArgumentException("摆动幅度超出范围");
            }
            if (!Float.isFinite(frequencyHz) || frequencyHz <= 0 || frequencyHz > 8) {
                throw new IllegalArgumentException("摆动频率超出范围");
            }
            if (!Float.isFinite(damping) || damping < 0 || damping > 1) {
                throw new IllegalArgumentException("摆动阻尼超出范围");
            }
        }

        public State toState(long revision) {
            return State.newBuilder()
                    .setProtocolVersion(PROTOCOL_VERSION)
                    .setEnabled(enabled)
                    .setAmplitudeDegrees(amplitudeDegrees)
                    .setFrequencyHz(frequencyHz)
                    .setDamping(damping)
                    .setRevision(revision)
                    .build();
        }
    }
}
