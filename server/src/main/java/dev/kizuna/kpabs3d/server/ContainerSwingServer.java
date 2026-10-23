package dev.kizuna.kpabs3d.server;

import com.google.protobuf.InvalidProtocolBufferException;

import dev.kizuna.kpabs3d.protocol.SwingMessages;
import dev.kizuna.kpabs3d.protocol.SwingMessages.SwingSettings;
import dev.kizuna.kpabs3d.protocol.pb.ContainerSwingProtocol.Hello;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 服务端权威发布摆动策略；客户端不能通过请求包修改幅度或频率。 */
public final class ContainerSwingServer implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(SwingMessages.MOD_ID);
    private static final SwingSettings SETTINGS = SwingMessages.defaults();

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(
                SwingMessages.HELLO,
                (server, player, handler, buffer, responseSender) -> {
                    try {
                        Hello hello = SwingMessages.read(buffer, Hello.parser());
                        SwingMessages.validate(hello);
                        server.execute(() -> sendState(player));
                    } catch (InvalidProtocolBufferException | IllegalArgumentException invalid) {
                        // 协议错误只拒绝本次握手，不让损坏的客户端输入进入世界状态。
                        LOG.warn("玩家 {} 的摆动握手无效：{}", player.getUuid(), invalid.getMessage());
                    }
                });
        LOG.info("容器摆动 protobuf 服务端已初始化");
    }

    private static void sendState(ServerPlayerEntity player) {
        // 回调已经回到服务端主线程；只向发起握手的连接返回权威快照。
        if (player.networkHandler.isConnectionOpen()
                && ServerPlayNetworking.canSend(player, SwingMessages.STATE)) {
            ServerPlayNetworking.send(
                    player, SwingMessages.STATE, SwingMessages.write(SETTINGS.toState(1)));
        }
    }
}
