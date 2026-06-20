package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import nekotori_haru.more_iss.entity.BeamWarningEntity;
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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 予告線レンダラー。
 * BaseBeamRendererの極細版。属性カラーの細い棒を、エンティティ位置から
 * 固定方向(直下)へdistance分だけ伸ばして描画する。
 * 当たり判定や着弾エフェクトは持たず、見た目のみ。
 */
public class BeamWarningRenderer extends EntityRenderer<BeamWarningEntity> {
    public static final ModelLayerLocation MODEL_LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("more_iss", "beam_warning_model"), "main");

    private static final ResourceLocation TEXTURE_CORE =
            ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/ray_of_frost/core.png");

    // ⭐ 本体ビームのbody(-6,-16,-6, 12,32,12)に対して大幅に縮小した極細の棒
    private static final float ROD_HALF_WIDTH = 0.6f;
    private static final float ROD_SEGMENT_LENGTH = 32f;

    private final ModelPart body;

    public BeamWarningRenderer(Context context) {
        super(context);
        ModelPart modelpart = context.bakeLayer(MODEL_LAYER_LOCATION);
        this.body = modelpart.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-ROD_HALF_WIDTH, -16, -ROD_HALF_WIDTH, ROD_HALF_WIDTH * 2, 32, ROD_HALF_WIDTH * 2),
                PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void render(BeamWarningEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        float lifetime = BeamWarningEntity.WARNING_DURATION;
        float scalar = 0.05f; // ⭐ 本体(0.25f)よりさらに細く
        float length = ROD_SEGMENT_LENGTH * scalar * scalar;
        float f = entity.tickCount + partialTicks;

        // 固定方向(常に直下)に向けてモデルを回転させる
        // direction は常に (0,-1,0) 固定のため、モデルのY軸方向(下方向)に
        // そのまま伸ばせばよく、追加の回転は不要(回転を加えると横倒しになってしまう)
        poseStack.scale(scalar, scalar, scalar);

        // 終盤にフェードアウトしていく予告線
        float alpha = 0.6f * Mth.clamp(1f - f / lifetime, 0.1f, 1f);
        int packedColor = entity.getBeamType().getColor();
        float r = ((packedColor >> 16) & 0xFF) / 255.0f;
        float g = ((packedColor >> 8) & 0xFF) / 255.0f;
        float b = (packedColor & 0xFF) / 255.0f;

        double totalLength = entity.getLength() * 4;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.energySwirl(TEXTURE_CORE, 0, 0));

        for (float i = 0; i < totalLength; i += length) {
            poseStack.translate(0, -length, 0);
            poseStack.pushPose();
            this.body.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BeamWarningEntity entity) {
        return TEXTURE_CORE;
    }
}