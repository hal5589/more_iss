package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
// ★★★ ここが重要：Color クラスをインポート ★★★
import nekotori_haru.more_iss.entity.NapalmBombEntity;
import nekotori_haru.more_iss.client.model.NapalmBombModel;

public class NapalmBombRenderer extends GeoEntityRenderer<NapalmBombEntity> {

    public NapalmBombRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NapalmBombModel());
        this.shadowRadius = 0.3f;
    }

    // ---------- 半透明レンダリングを有効化 ----------
    @Override
    public RenderType getRenderType(NapalmBombEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    // ---------- 色と透明度（半透明）を設定 ----------
    @Override
    public Color getRenderColor(NapalmBombEntity animatable, float partialTick, int packedLight) {
        // RGBA で白色・透明度 0.8 を指定（0.8 * 255 = 204）
        return Color.ofRGBA(255, 255, 255, 80);
    }

    // ---------- 描画時に強制的に最大明るさ（フルブライト）にする ----------
    @Override
    public void render(NapalmBombEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 15728640 = フルブライト（最大明るさ）のパック値
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, 15728640);
    }

    // ---------- 角度制御（着地後は固定、飛翔中は速度方向） ----------
    @Override
    public void preRender(PoseStack poseStack, NapalmBombEntity entity, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        // スーパーの前処理（必須）
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // エンティティの中心に移動（剣の持ち手位置調整）
        poseStack.translate(0, entity.getBbHeight() * 0.5f, 0);

        // 着地後は保存された角度を使用、飛翔中は速度方向を向く
        if (entity.isAngleSaved()) {
            // 着地直前に保存した角度を適用（それ以降は動かない）
            float pitch = entity.getSavedPitch();
            float yaw = entity.getSavedYaw();
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        } else {
            // 飛翔中：速度ベクトルに合わせて回転（放物線に沿う）
            Vec3 motion = entity.prevMotion.lerp(entity.getDeltaMovement(), partialTick);
            if (motion.lengthSqr() > 0.001) {
                float pitch = ((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * (180F / Math.PI)) - 90.0F);
                float yaw = -((float) (Mth.atan2(motion.z, motion.x) * (180F / Math.PI)) - 90.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            }
        }
    }
}