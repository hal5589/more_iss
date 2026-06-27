package nekotori_haru.more_iss.client.renderer;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.EternalWizardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class EternalWizardRenderer extends HumanoidMobRenderer<EternalWizardEntity, HumanoidModel<EternalWizardEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(More_iss.MODID, "textures/entity/eternal_wizard.png");

    public EternalWizardRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);

        // ⭐ 防具レイヤーを追加（装備を表示するために必須）
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(EternalWizardEntity entity) {
        return TEXTURE;
    }
}