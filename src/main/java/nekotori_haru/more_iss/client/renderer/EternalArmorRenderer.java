package nekotori_haru.more_iss.client.renderer;

import nekotori_haru.more_iss.client.model.EternalArmorModel;
import nekotori_haru.more_iss.item.armor.EternalArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class EternalArmorRenderer extends GeoArmorRenderer<EternalArmorItem> {

    public EternalArmorRenderer() {
        // EternalArmorModel は GeoModel<EternalArmorItem> を継承しているため、
        // GeoArmorRenderer<EternalArmorItem> のモデル引数として型が一致する
        super(new EternalArmorModel());
    }
}