package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nekotori_haru.more_iss.entity.PolychromaticBeamEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PolychromaticBeamRenderer extends EntityRenderer<PolychromaticBeamEntity> {
    private static final ResourceLocation BEAM_TEX = new ResourceLocation("textures/entity/beacon_beam.png");

    public PolychromaticBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(PolychromaticBeamEntity entity) {
        return BEAM_TEX;
    }

    @Override
    public void render(PolychromaticBeamEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vector3f from = new Vector3f(0, 1, 0);
        Vector3f to = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        Quaternionf rotation = new Quaternionf().rotationTo(from, to);

        float length = entity.getLength();
        float radius = entity.getRadius();

        // 虹色グラデーション
        float time = (entity.tickCount + partialTicks) * 0.02f;
        float r = (float)(Math.sin(time) * 0.5 + 0.5);
        float g = (float)(Math.sin(time + 2.0) * 0.5 + 0.5);
        float b = (float)(Math.sin(time + 4.0) * 0.5 + 0.5);
        float a = 0.85f;

        int outerColor = ( ((int)(a * 255)) << 24 ) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | ((int)(b * 255));
        int innerColor = ( ((int)(a * 255)) << 24 ) | ((int)(r * 0.7f * 255) << 16) | ((int)(g * 0.7f * 255) << 8) | ((int)(b * 0.7f * 255));

        float uvScroll = (entity.tickCount + partialTicks) * 0.25f;

        VertexConsumer consumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEX, false));

        // 外側ビーム
        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 7.1f));
        drawBeam(poseStack, consumer, length, radius, outerColor, packedLight, uvScroll);
        poseStack.popPose();

        // 内側ビーム
        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 3.3f));
        drawBeam(poseStack, consumer, length, radius * 0.7f, innerColor, packedLight, uvScroll);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawBeam(PoseStack poseStack, VertexConsumer consumer, float length, float radius,
                          int argb, int packedLight, float uvScroll) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        float v0 = -uvScroll;
        float v1 = v0 + length;

        float x0 = -radius;
        float x1 = radius;
        float z0 = -radius;
        float z1 = radius;
        float y0 = 0.0f;
        float y1 = length;

        Matrix4f poseMat = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        // 6面のQuad（片面のみ）
        addQuad(poseMat, normalMat, consumer,
                x1, y0, z0, 0f, v0,
                x1, y1, z0, 0f, v1,
                x1, y1, z1, 1f, v1,
                x1, y0, z1, 1f, v0,
                r, g, b, a, packedLight, 1f, 0f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z1, 0f, v0,
                x0, y1, z1, 0f, v1,
                x0, y1, z0, 1f, v1,
                x0, y0, z0, 1f, v0,
                r, g, b, a, packedLight, -1f, 0f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x1, y0, z1, 0f, v0,
                x1, y1, z1, 0f, v1,
                x0, y1, z1, 1f, v1,
                x0, y0, z1, 1f, v0,
                r, g, b, a, packedLight, 0f, 0f, 1f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z0, 0f, v0,
                x0, y1, z0, 0f, v1,
                x1, y1, z0, 1f, v1,
                x1, y0, z0, 1f, v0,
                r, g, b, a, packedLight, 0f, 0f, -1f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z0, 0f, 0f,
                x0, y0, z1, 0f, 1f,
                x1, y0, z1, 1f, 1f,
                x1, y0, z0, 1f, 0f,
                r, g, b, a, packedLight, 0f, -1f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x1, y1, z0, 0f, 0f,
                x1, y1, z1, 0f, 1f,
                x0, y1, z1, 1f, 1f,
                x0, y1, z0, 1f, 0f,
                r, g, b, a, packedLight, 0f, 1f, 0f);
    }

    private void addQuad(Matrix4f poseMat, Matrix3f normalMat, VertexConsumer vc,
                         float x0, float y0, float z0, float u0, float v0,
                         float x1, float y1, float z1, float u1, float v1,
                         float x2, float y2, float z2, float u2, float v2,
                         float x3, float y3, float z3, float u3, float v3,
                         float r, float g, float b, float a, int light,
                         float nx, float ny, float nz) {
        vc.vertex(poseMat, x0, y0, z0).color(r, g, b, a).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();
        vc.vertex(poseMat, x1, y1, z1).color(r, g, b, a).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();
        vc.vertex(poseMat, x2, y2, z2).color(r, g, b, a).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();
        vc.vertex(poseMat, x3, y3, z3).color(r, g, b, a).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();
    }
}