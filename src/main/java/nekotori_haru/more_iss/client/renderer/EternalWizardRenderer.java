package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.EternalWizardEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;

public class EternalWizardRenderer extends LivingEntityRenderer<EternalWizardEntity, PlayerModel<EternalWizardEntity>> {

    private static final ResourceLocation DUMMY_TEXTURE = new ResourceLocation(More_iss.MODID, "textures/entity/eternal_wizard.png");

    public EternalWizardRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(EternalWizardEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public void render(EternalWizardEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // 虹色の計算
        long time = System.currentTimeMillis();
        float hue = (time % 7200) / 7200f;
        float saturation = 0.15f;
        float brightness = 1.0f;
        int color = Color.HSBtoRGB(hue, saturation, brightness);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // スケールと位置調整
        float f = 0.9375F;
        poseStack.scale(f, f, f);
        poseStack.translate(0.0D, -1.501F, 0.0D);

        // モデルを描画
        PlayerModel<EternalWizardEntity> model = this.getModel();
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, entityYaw, entity.getXRot());

        float alpha = 1.0f;
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(DUMMY_TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    // ⭐ 浮遊アニメーションは tick 内で処理するため、setupRotations はオーバーライドしない
}