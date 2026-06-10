package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.client.model.GlacialExecution;
import nekotori_haru.more_iss.entity.GlacialSwordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class GlacialSwordRenderer extends EntityRenderer<GlacialSwordEntity> {
    private final GlacialExecution<GlacialSwordEntity> model;

    public GlacialSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GlacialExecution<>(context.bakeLayer(GlacialExecution.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(GlacialSwordEntity entity) {
        return new ResourceLocation(More_iss.MODID, "textures/block/ice.png");
    }

    @Override
    public void render(GlacialSwordEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0, 0.5, 0);

        float xRot = entity.getTargetXRot();
        float yRot = entity.getYRot();

        // 1. Yaw：Minecraftの座標系に合わせてマイナス
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yRot));
        // 2. Pitch：振り下ろし角度
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
        // 3. Roll：剣を縦に持つ（傾き補正なし）
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0.0f));

        float scale = 13.0f;
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, vertexConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.5f, 0.8f, 1.0f, 1.0f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}