package nekotori_haru.more_iss.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import nekotori_haru.more_iss.entity.NapalmBombEntity;

public class NapalmBombModel extends GeoModel<NapalmBombEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation("irons_spellbooks", "geo/fiery_dagger.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("irons_spellbooks", "textures/entity/fiery_dagger.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("more_iss", "animations/dummy.animation.json");

    @Override
    public ResourceLocation getModelResource(NapalmBombEntity object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NapalmBombEntity object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NapalmBombEntity animatable) {
        return ANIMATION;
    }
}