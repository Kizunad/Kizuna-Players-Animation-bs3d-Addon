package dev.kizuna.kpabs3d.client;

import dev.kizuna.backpack3d.client.ModelAsset;
import dev.kizuna.backpack3d.protocol.pb.VolumeProtocol.Mount;
import dev.kizuna.kpabs3d.protocol.SwingMessages;
import dev.kizuna.kpabs3d.protocol.SwingMessages.SwingSettings;
import dev.kizuna.playeranimation.client.PlayerAnimationClient;
import dev.kizuna.playeranimation.client.PlayerMeshRenderer;
import dev.kizuna.playeranimation.model.BoneId;
import dev.kizuna.playeranimation.model.Pose;
import dev.kizuna.playeranimation.model.Rig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** 在 BS3D 的标准挂点前叠加 KPA 骨骼增量与简短的惯性摆动。 */
public final class SwingingContainerTransform {
    private static final float MODEL_UNITS_PER_BLOCK = 16;
    private static final float POUCH_SCALE = 0.5f;
    private static final float BASKET_SCALE = 0.8f;
    private static final float WAIST_X = 0.42f;
    private static final float WAIST_Z = 0.29f;
    private static final float WAIST_Y = 0.65f;
    private static final float TORSO_Y = 0.38f;
    private static final float TORSO_Z = 0.36f;
    private static final float FRONT_DEGREES = 180;
    private static final float SWAY_SPEED_SCALE = 6;
    private static final float RADIANS_PER_SECOND = (float) (Math.PI * 2);

    private SwingingContainerTransform() {
    }

    public static void apply(
            MatrixStack matrices,
            ModelAsset asset,
            boolean waist,
            Mount mount,
            float angleDegrees,
            AbstractClientPlayerEntity player) {
        // WorldEquipmentRenderer 只会传入已穿戴且已登记的 BS3D 模型；因此所有 BS3D 容器
        // 共用同一套 KPA 骨骼增量和惯性摆动，不按模型 ID 设白名单。
        boolean swing =
                asset != null
                        && player != null
                        && ContainerSwingClient.settings().enabled();
        if (swing) {
            applyAnimationDelta(matrices, player, waist);
        }
        applyBaseMount(matrices, asset, waist, mount, angleDegrees);
        if (swing) {
            // 基础挂点完成后再旋转，摆动轴位于容器挂带而不是玩家身体原点。
            applyInertialSway(matrices, player, ContainerSwingClient.settings());
        }
    }

    private static void applyAnimationDelta(
            MatrixStack matrices, AbstractClientPlayerEntity player, boolean waist) {
        PlayerMeshRenderer.Frame frame = PlayerAnimationClient.PLAYBACK.frame(player.getUuid());
        if (frame == null) {
            return;
        }
        EntityRenderer<?> renderer =
                MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerEntityRenderer playerRenderer)) {
            return;
        }
        PlayerEntityModel<?> vanilla = playerRenderer.getModel();
        boolean slim = "slim".equals(player.getModel());
        Pose[] basePose = PlayerMeshRenderer.basePose(frame.model().skeleton(), vanilla, false, slim);
        Matrix4f[] baseMatrices = Rig.matrices(frame.model().skeleton(), basePose);
        BoneId targetBone = waist ? BoneId.WAIST : BoneId.CHEST;
        Matrix4f baseBody = Rig.toVanilla(baseMatrices[BoneId.CHEST.ordinal()]);
        Matrix4f animatedBone = Rig.toVanilla(frame.matrices()[targetBone.ordinal()]);
        Matrix4f delta = new Matrix4f(baseBody).invert().mul(animatedBone);
        // BS3D 的 body.rotate 已提供原版基准姿态，这里只追加 KPA 动画相对基准的变换。
        matrices.peek().getPositionMatrix().mul(delta);
        matrices.peek().getNormalMatrix().mul(new Matrix3f(delta));
    }

    private static void applyInertialSway(
            MatrixStack matrices, AbstractClientPlayerEntity player, SwingSettings settings) {
        net.minecraft.util.math.Vec3d velocity = player.getVelocity();
        float speed = MathHelper.clamp((float) velocity.horizontalLength() * SWAY_SPEED_SCALE, 0, 1);
        if (speed <= 0) {
            return;
        }
        float time = player.age / 20.0f;
        float phase = time * settings.frequencyHz() * RADIANS_PER_SECOND;
        float damping = 0.35f + settings.damping() * 0.65f;
        float amplitude = settings.amplitudeDegrees() * speed * damping;
        float pitch = MathHelper.sin(phase) * amplitude;
        float roll = MathHelper.cos(phase * 0.5f) * amplitude * 0.45f;
        // 玩家模型空间的 Y 轴向下；绕 X 的符号与 KPA 的模型空间转换保持一致。
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }

    private static void applyBaseMount(
            MatrixStack matrices,
            ModelAsset asset,
            boolean waist,
            Mount mount,
            float angleDegrees) {
        // 复制 BS3D 的最小挂点公式，保证替换调用后普通容器仍与原实现完全一致。
        float scale = (waist ? POUCH_SCALE : BASKET_SCALE) / MODEL_UNITS_PER_BLOCK;
        if (waist) {
            double angle = Math.toRadians(angleDegrees);
            float centerY = WAIST_Y + asset.topOffset() * scale;
            matrices.translate(Math.sin(angle) * WAIST_X, centerY, -Math.cos(angle) * WAIST_Z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(FRONT_DEGREES - angleDegrees));
        } else {
            boolean front = mount == Mount.TORSO_FRONT;
            matrices.translate(0, TORSO_Y, front ? -TORSO_Z : TORSO_Z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(front ? FRONT_DEGREES : 0));
        }
        // bbmodel 的 Y 轴向上，玩家模型和世界挂点的 Y 轴向下。
        matrices.scale(scale, -scale, -scale);
    }
}
