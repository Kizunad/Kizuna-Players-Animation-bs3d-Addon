package dev.kizuna.kpabs3d.client.mixin;

import dev.kizuna.backpack3d.client.ModelAsset;
import dev.kizuna.backpack3d.protocol.pb.VolumeProtocol.Mount;
import dev.kizuna.kpabs3d.client.SwingingContainerTransform;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 将 KPA 当前帧传给 BS3D 的既有世界装备渲染，不复制第二套容器特征渲染器。 */
@Mixin(targets = "dev.kizuna.backpack3d.client.WorldEquipmentRenderer")
public abstract class WorldEquipmentRendererMixin {
    @Unique private AbstractClientPlayerEntity kpaBs3d$player;

    @Inject(method = "render", at = @At("HEAD"))
    private void kpaBs3d$capture(
            AbstractClientPlayerEntity player,
            MatrixStack body,
            VertexConsumerProvider buffers,
            int light,
            CallbackInfo callback) {
        kpaBs3d$player = player;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kpaBs3d$release(
            AbstractClientPlayerEntity player,
            MatrixStack body,
            VertexConsumerProvider buffers,
            int light,
            CallbackInfo callback) {
        kpaBs3d$player = null;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/kizuna/backpack3d/client/WornModelTransform;apply(Lnet/minecraft/client/util/math/MatrixStack;Ldev/kizuna/backpack3d/client/ModelAsset;ZLdev/kizuna/backpack3d/protocol/pb/VolumeProtocol$Mount;F)V"))
    private void kpaBs3d$applyAnimatedMount(
            MatrixStack matrices,
            ModelAsset asset,
            boolean waist,
            Mount mount,
            float angleDegrees) {
        SwingingContainerTransform.apply(
                matrices, asset, waist, mount, angleDegrees, kpaBs3d$player);
    }
}
