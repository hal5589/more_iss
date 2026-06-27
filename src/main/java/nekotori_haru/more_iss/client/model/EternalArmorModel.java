package nekotori_haru.more_iss.client.model;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.item.armor.EternalArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// ⭐ GeckoLib4のArmor用GeoModelは「LivingEntity」ではなく
//    GeoArmorRenderer<EternalArmorItem> に合わせて EternalArmorItem(=GeoAnimatable)を型引数に取る
public class EternalArmorModel extends GeoModel<EternalArmorItem> {

    @Override
    public ResourceLocation getModelResource(EternalArmorItem animatable) {
        return new ResourceLocation(More_iss.MODID, "geo/eternal_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EternalArmorItem animatable) {
        return new ResourceLocation(More_iss.MODID, "textures/armor/eternal_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EternalArmorItem animatable) {
        return new ResourceLocation(More_iss.MODID, "animations/eternalarmor.animation.json");
    }
}