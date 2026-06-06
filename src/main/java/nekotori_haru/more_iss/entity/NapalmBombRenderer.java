package nekotori_haru.more_iss.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import nekotori_haru.more_iss.entity.NapalmBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class NapalmBombRenderer extends EntityRenderer<NapalmBombEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    public NapalmBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NapalmBombEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float time = (entity.tickCount + partialTick) * 0.5f;
        // 着地後は振動、飛翔中は回転
        float scale = 0.18f + (float) Math.sin(time * Math.PI) * 0.04f;

        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 8f));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));

        // 外側：オレンジの立方体
        renderCube(consumer, poseStack.last().pose(), 0.5f, 1.0f, 0.5f, 0.15f, packedLight);

        // 内側：白い小さい立方体
        poseStack.pushPose();
        poseStack.scale(0.55f, 0.55f, 0.55f);
        renderCube(consumer, poseStack.last().pose(), 0.5f, 1.0f, 1.0f, 1.0f, packedLight);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderCube(VertexConsumer consumer, Matrix4f matrix,
                            float h, float r, float g, float b, int light) {
        // 上面
        renderFace(consumer, matrix, -h, h, -h,  h, h, -h,  h, h,  h,  -h, h,  h,  r, g, b, light);
        // 下面
        renderFace(consumer, matrix, -h, -h,  h,  h, -h,  h,  h, -h, -h,  -h, -h, -h,  r, g, b, light);
        // 北面
        renderFace(consumer, matrix, -h,  h, -h,  -h, -h, -h,   h, -h, -h,   h,  h, -h,  r, g, b, light);
        // 南面
        renderFace(consumer, matrix,  h,  h,  h,   h, -h,  h,  -h, -h,  h,  -h,  h,  h,  r, g, b, light);
        // 西面
        renderFace(consumer, matrix, -h,  h,  h,  -h, -h,  h,  -h, -h, -h,  -h,  h, -h,  r, g, b, light);
        // 東面
        renderFace(consumer, matrix,  h,  h, -h,   h, -h, -h,   h, -h,  h,   h,  h,  h,  r, g, b, light);
    }

    private void renderFace(VertexConsumer consumer, Matrix4f matrix,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float r, float g, float b, int light) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, 1.0f).uv(0, 0).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).uv(0, 1).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).uv(1, 1).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, 1.0f).uv(1, 0).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(NapalmBombEntity entity) {
        return TEXTURE;
    }
}