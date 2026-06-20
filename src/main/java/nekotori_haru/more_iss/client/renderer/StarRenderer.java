package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import nekotori_haru.more_iss.entity.StarEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class StarRenderer extends EntityRenderer<StarEntity> {

    // ★ Vanillaの純白テクスチャ (1px, 陰影・色ムラなし) に変更 ★
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final int MAX_LIFE = 80;
    // ★ 0xF000F0 は不正な値だったため、正規のpack(15,15)に修正 ★
    private static final int FULL_BRIGHT = LightTexture.pack(15, 15); // 最大輝度

    public StarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StarEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float time = (entity.tickCount + partialTick);
        float lifeRatio = Mth.clamp(entity.tickCount / (float) MAX_LIFE, 0.0f, 1.0f);
        float pulse = 0.8f + 0.2f * Mth.sin(time * 0.3f);

        float grow = 1.0f + lifeRatio * 0.3f;
        float scale = 0.25f * pulse * grow;

        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 5f));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 3f));

        int fullBrightLight = FULL_BRIGHT;

        // ★ 完全な白 (透明度80% - 外側) ★
        VertexConsumer outerConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        renderGlowSphere(outerConsumer, poseStack.last().pose(),
                1.0f, 1.0f, 1.0f, 0.8f, fullBrightLight, 1.0f);

        // ★ 完全な白 (透明度100% - 中心) ★
        VertexConsumer innerConsumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        renderGlowSphere(innerConsumer, poseStack.last().pose(),
                1.0f, 1.0f, 1.0f, 1.0f, fullBrightLight, 0.4f);

        // ★ 光のオーラ (完全な白 - 透明度60%) ★
        VertexConsumer auraConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        renderGlowSphere(auraConsumer, poseStack.last().pose(),
                1.0f, 1.0f, 1.0f, 0.6f, fullBrightLight, 1.6f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderGlowSphere(VertexConsumer consumer, Matrix4f matrix,
                                  float r, float g, float b, float alpha,
                                  int light, float size) {
        float h = size * 0.5f;

        // 上面 (+Y)
        renderFace(consumer, matrix, -h, h, -h,  h, h, -h,  h, h,  h,  -h, h,  h,  r, g, b, alpha, light, 0, -1, 0);
        // 下面 (-Y)
        renderFace(consumer, matrix, -h, -h,  h,  h, -h,  h,  h, -h, -h,  -h, -h, -h,  r, g, b, alpha, light, 0, 1, 0);
        // 北面 (-Z)
        renderFace(consumer, matrix, -h,  h, -h,  -h, -h, -h,   h, -h, -h,   h,  h, -h,  r, g, b, alpha, light, 0, 0, 1);
        // 南面 (+Z)
        renderFace(consumer, matrix,  h,  h,  h,   h, -h,  h,  -h, -h,  h,  -h,  h,  h,  r, g, b, alpha, light, 0, 0, -1);
        // 西面 (-X)
        renderFace(consumer, matrix, -h,  h,  h,  -h, -h,  h,  -h, -h, -h,  -h,  h, -h,  r, g, b, alpha, light, 1, 0, 0);
        // 東面 (+X)
        renderFace(consumer, matrix,  h,  h, -h,   h, -h, -h,   h, -h,  h,   h,  h,  h,  r, g, b, alpha, light, -1, 0, 0);
    }

    private void renderFace(VertexConsumer consumer, Matrix4f matrix,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float r, float g, float b, float alpha, int light,
                            float nx, float ny, float nz) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, alpha).uv(0, 0).overlayCoords(0).uv2(light).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha).uv(0, 1).overlayCoords(0).uv2(light).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha).uv(1, 1).overlayCoords(0).uv2(light).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, alpha).uv(1, 0).overlayCoords(0).uv2(light).normal(nx, ny, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(StarEntity entity) {
        return TEXTURE;
    }
}