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
 * 属性カラーの細い棒を、エンティティ位置から方向ベクトルに沿ってdistance分だけ伸ばして描画する。
 */
public class BeamWarningRenderer extends EntityRenderer<BeamWarningEntity> {
    public static final ModelLayerLocation MODEL_LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("more_iss", "beam_warning_model"), "main");

    private static final ResourceLocation TEXTURE_CORE =
            ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/ray_of_frost/core.png");

    // モデルサイズ（ピクセル単位）
    private static final float ROD_HALF_WIDTH = 0.6f;
    private static final float ROD_SEGMENT_HEIGHT = 32f;  // モデルの高さ

    // レンダリングスケール（視認性を確保）
    private static final float RENDER_SCALE = 0.15f;

    // セグメント間隔（モデル高さの半分で重ねて連続的に見せる）
    private static final float SEGMENT_STEP = ROD_SEGMENT_HEIGHT * 0.5f;  // 16

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
                        .addBox(-ROD_HALF_WIDTH, -16, -ROD_HALF_WIDTH, ROD_HALF_WIDTH * 2, ROD_SEGMENT_HEIGHT, ROD_HALF_WIDTH * 2),
                PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void render(BeamWarningEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        float f = entity.tickCount + partialTicks;

        // スケール適用
        poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);

        // フェードアウト（寿命に応じて）
        float lifetime = BeamWarningEntity.WARNING_DURATION;
        float alpha = 0.8f * Mth.clamp(1f - f / lifetime, 0.1f, 1f);

        // 属性カラー
        int packedColor = entity.getBeamType().getColor();
        float r = ((packedColor >> 16) & 0xFF) / 255.0f;
        float g = ((packedColor >> 8) & 0xFF) / 255.0f;
        float b = (packedColor & 0xFF) / 255.0f;

        // ⭐ セグメント数計算（間隔を SEGMENT_STEP にして短距離でも複数セグメント表示）
        double beamLength = entity.getLength();
        float stepWorld = SEGMENT_STEP * RENDER_SCALE;  // 実際のワールド座標での間隔
        int segments = (int) Math.ceil(beamLength / stepWorld);
        // 最低でも2セグメントは表示（短すぎるときは2つで間隔を調整）
        if (segments < 2) segments = 2;

        // デバッグログ（必要に応じてコメントアウト）
        // System.out.println("[BeamWarning] segments=" + segments + ", length=" + beamLength);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.energySwirl(TEXTURE_CORE, 0, 0));

        // モデルを上から下に向かって積み重ねる（Y軸負方向）
        // 方向ベクトルが真下（0,-1,0）であることを前提
        for (int i = 0; i < segments; i++) {
            poseStack.translate(0, -SEGMENT_STEP, 0);
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