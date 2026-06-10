package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import nekotori_haru.more_iss.entity.BaseBeamVisualEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BaseBeamRenderer extends EntityRenderer<BaseBeamVisualEntity> {
    public static final ModelLayerLocation MODEL_LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("more_iss", "base_beam_model"), "main");

    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/ray_of_frost/core.png");
    private static final ResourceLocation TEXTURE_OVERLAY = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/ray_of_frost/overlay.png");

    private final ModelPart body;

    public BaseBeamRenderer(Context context) {
        super(context);
        ModelPart modelpart = context.bakeLayer(MODEL_LAYER_LOCATION);
        this.body = modelpart.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -16, -6, 12, 32, 12), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public boolean shouldRender(BaseBeamVisualEntity entity, Frustum camera, double x, double y, double z) {
        if (super.shouldRender(entity, camera, x, y, z)) return true;
        if (entity.getOwner() != null) {
            Vec3 vec3 = entity.getPosition(0.0F);
            Vec3 vec31 = entity.getOwner().getEyePosition(0.0F);
            return camera.isVisible(entity.getBoundingBox().expandTowards(vec3.subtract(vec31).scale(2.0D)));
        }
        return false;
    }

    @Override
    public void render(BaseBeamVisualEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
        float lifetime = BaseBeamVisualEntity.lifetime;
        float scalar = .25f;
        float length = 32 * scalar * scalar;
        float f = entity.tickCount + partialTicks;

        poseStack.translate(0, 0.1, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot() - 90));
        poseStack.scale(scalar, scalar, scalar);

        float alpha = 0.5f * Mth.clamp(1f - f / lifetime, 0, 1);
        int packedColor = entity.getBeamType().getColor();
        float r = ((packedColor >> 16) & 0xFF) / 255.0f;
        float g = ((packedColor >> 8) & 0xFF) / 255.0f;
        float b = (packedColor & 0xFF) / 255.0f;

        for (float i = 0; i < entity.distance * 4; i += length) {
            poseStack.translate(0, length, 0);

            // 1. 【外周】半透明オーバーレイ
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.energySwirl(TEXTURE_OVERLAY, 0, 0));
            {
                poseStack.pushPose();
                float expansion = Mth.clampedLerp(1.2f, 0, f / lifetime);
                poseStack.mulPose(Axis.YP.rotationDegrees(f * 8));
                poseStack.scale(expansion, 1, expansion);
                this.body.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
                poseStack.popPose();
            }

            // 2. 【中間】属性カラーコア
            consumer = bufferSource.getBuffer(RenderType.energySwirl(TEXTURE_CORE, 0, 0));
            {
                poseStack.pushPose();
                float expansion = Mth.clampedLerp(0.9f, 0, f / lifetime);
                poseStack.scale(expansion, 1, expansion);
                poseStack.mulPose(Axis.YP.rotationDegrees(f * -12));
                this.body.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
                poseStack.popPose();
            }

            // 3. 【中心】ホワイトコア（ほぼ白で細く描画）
            {
                poseStack.pushPose();
                float expansion = Mth.clampedLerp(0.35f, 0, f / lifetime); // 細く設定
                poseStack.scale(expansion, 1, expansion);
                poseStack.mulPose(Axis.YP.rotationDegrees(f * -12));
                // 純白 (1.0f, 1.0f, 1.0f) かつ不透明度を高めに
                this.body.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 0.9f);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    @Override public ResourceLocation getTextureLocation(BaseBeamVisualEntity entity) { return TEXTURE_CORE; }
}