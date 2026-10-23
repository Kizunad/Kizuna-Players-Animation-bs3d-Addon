package dev.kizuna.kpabs3d.client;

import com.google.protobuf.InvalidProtocolBufferException;

import dev.kizuna.kpabs3d.protocol.SwingMessages;
import dev.kizuna.kpabs3d.protocol.SwingMessages.SwingSettings;
import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.Hello;
import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.State;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端只消费服务端策略，并将状态交给世界容器渲染变换。 */
public final class ContainerSwingClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(SwingMessages.MOD_ID);
    private static volatile SwingSettings settings = SwingMessages.disabled();
    private static volatile long revision;
    private static volatile boolean helloPending;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SwingMessages.STATE,
                (client, handler, buffer, responseSender) -> {
                    try {
                        State state = SwingMessages.read(buffer, State.parser());
                        SwingSettings decoded = SwingMessages.validate(state);
                        client.execute(() -> accept(state, decoded));
                    } catch (InvalidProtocolBufferException | IllegalArgumentException invalid) {
                        LOG.warn("服务端摆动状态无效：{}", invalid.getMessage());
                    }
                });
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    settings = SwingMessages.disabled();
                    revision = 0;
                    helloPending = true;
                    sendHello();
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> sendHello());
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    settings = SwingMessages.disabled();
                    revision = 0;
                    helloPending = false;
                });
        LOG.info("容器摆动 protobuf 客户端已初始化");
    }

    public static SwingSettings settings() {
        return settings;
    }

    private static void sendHello() {
        // JOIN 可能早于服务器通道注册；保留一次待发送握手，避免把连接顺序当成协议失败。
        if (!helloPending || !ClientPlayNetworking.canSend(SwingMessages.HELLO)) {
            return;
        }
        ClientPlayNetworking.send(
                SwingMessages.HELLO,
                SwingMessages.write(
                        Hello.newBuilder()
                                .setProtocolVersion(SwingMessages.PROTOCOL_VERSION)
                                .build()));
        helloPending = false;
    }

    private static void accept(State state, SwingSettings decoded) {
        // 旧修订不能覆盖重连后的新世界；服务端当前只使用单调递增修订号。
        if (state.getRevision() < revision) {
            return;
        }
        revision = state.getRevision();
        settings = decoded;
    }
}
