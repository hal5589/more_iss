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

    // ========== 調整パラメータ ==========
    private static final float SCALE = 0.8f;           // 全体の大きさ
    private static final float LENGTH = 6.0f;          // 槍の長さ
    private static final float WIDTH = 1.2f;           // 槍の幅
    private static final float UV_OFFSET_X = 0.5f;     // UVのX中心位置
    private static final float UV_TOP = 1.0f;          // 先端のUV (上端)
    private static final float UV_BOTTOM = 0.0f;       // 柄のUV (下端)
    private static final float ANGLE = 0.0f;           // クロス角度
    // =================================

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

        Vec3 motion = entity.getDeltaMovement();
        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * (180F / Math.PI)) - 90.0F);
        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * (180F / Math.PI)) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        poseStack.scale(SCALE, SCALE, SCALE);

        renderModel(poseStack, bufferSource);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float halfWidth = WIDTH;
        float halfHeight = LENGTH;

        float uvMinX = UV_OFFSET_X - 0.5f;
        float uvMaxX = UV_OFFSET_X + 0.5f;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

        // ===== 垂直平面 =====
        poseStack.mulPose(Axis.XP.rotationDegrees(ANGLE));

        // 頂点1 (左下 - 先端側) → テクスチャ下端
        consumer.vertex(poseMatrix, 0, -halfWidth, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, UV_BOTTOM)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点2 (右下 - 先端側)
        consumer.vertex(poseMatrix, 0, halfWidth, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, UV_BOTTOM)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点3 (右上 - 柄側)
        consumer.vertex(poseMatrix, 0, halfWidth, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, UV_TOP)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点4 (左上 - 柄側)
        consumer.vertex(poseMatrix, 0, -halfWidth, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, UV_TOP)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        poseStack.mulPose(Axis.XP.rotationDegrees(-ANGLE));

        // ===== 水平平面 =====
        poseStack.mulPose(Axis.YP.rotationDegrees(-ANGLE));

        // 頂点1 (左下 - 先端側)
        consumer.vertex(poseMatrix, -halfWidth, 0, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, UV_BOTTOM)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点2 (右下 - 先端側)
        consumer.vertex(poseMatrix, halfWidth, 0, -halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, UV_BOTTOM)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点3 (右上 - 柄側)
        consumer.vertex(poseMatrix, halfWidth, 0, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMaxX, UV_TOP)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        // 頂点4 (左上 - 柄側)
        consumer.vertex(poseMatrix, -halfWidth, 0, halfHeight)
                .color(255, 255, 255, 255)
                .uv(uvMinX, UV_TOP)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();

        poseStack.mulPose(Axis.YP.rotationDegrees(ANGLE));
    }
}