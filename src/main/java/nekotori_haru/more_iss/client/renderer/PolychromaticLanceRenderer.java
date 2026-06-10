package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.PolychromaticLanceEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PolychromaticLanceRenderer extends EntityRenderer<PolychromaticLanceEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(More_iss.MODID, "textures/entity/polychromatic_lance.png");

    // ========== 自分で調整できるパラメータ ==========
    private static final float SCALE = 0.8f;           // 全体の大きさ
    private static final float LENGTH = 6.0f;          // 槍の長さ (大きいほど長い)
    private static final float WIDTH = 1.2f;           // 槍の幅 (大きいほど太い)
    private static final float UV_OFFSET_X = 0.5f;     // UVのX中心位置 (0.0～1.0) テクスチャの中心ずらす場合
    private static final float UV_TOP = 1.0f;          // 先端のUV (下端)
    private static final float UV_BOTTOM = 0.0f;       // 柄のUV (上端)    // 柄のUV (0.0～1.0) 大きいほどテクスチャの下側
    private static final float ANGLE = 0.0f;          // クロスする角度 (0～90)
    // =============================================

    public PolychromaticLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PolychromaticLanceEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PolychromaticLanceEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        // 移動方向に基づいて回転
        Vec3 motion = entity.getDeltaMovement();
        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * (180F / Math.PI)) - 90.0F);
        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * (180F / Math.PI)) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        // スケール調整
        poseStack.scale(SCALE, SCALE, SCALE);

        // モデル描画
        renderModel(poseStack, bufferSource);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        // テクスチャUV設定
        float uvMinX = UV_OFFSET_X - 0.5f;
        float uvMaxX = UV_OFFSET_X + 0.5f;
        float uvTop = UV_TOP;
        float uvBottom = UV_BOTTOM;

        // 槍のサイズ
        float halfWidth = WIDTH;
        float halfHeight = LENGTH;

        // 透過対応のRenderTypeを使用
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

        // ===== 垂直平面 =====
        poseStack.mulPose(Axis.XP.rotationDegrees(ANGLE));

        // 頂点1 (左下 - 柄側)
        consumer.vertex(poseMatrix, 0, -halfWidth, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, uvBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点2 (右下 - 柄側)
        consumer.vertex(poseMatrix, 0, halfWidth, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, uvBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点3 (右上 - 先端側)
        consumer.vertex(poseMatrix, 0, halfWidth, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, uvTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点4 (左上 - 先端側)
        consumer.vertex(poseMatrix, 0, -halfWidth, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, uvTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        poseStack.mulPose(Axis.XP.rotationDegrees(-ANGLE));

        // ===== 水平平面 =====
        poseStack.mulPose(Axis.YP.rotationDegrees(-ANGLE));

        // 頂点1 (左下 - 柄側)
        consumer.vertex(poseMatrix, -halfWidth, 0, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, uvBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点2 (右下 - 柄側)
        consumer.vertex(poseMatrix, halfWidth, 0, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, uvBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点3 (右上 - 先端側)
        consumer.vertex(poseMatrix, halfWidth, 0, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, uvTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点4 (左上 - 先端側)
        consumer.vertex(poseMatrix, -halfWidth, 0, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, uvTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        poseStack.mulPose(Axis.YP.rotationDegrees(ANGLE));
    }
}