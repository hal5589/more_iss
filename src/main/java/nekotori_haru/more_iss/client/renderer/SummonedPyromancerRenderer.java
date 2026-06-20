package nekotori_haru.more_iss.client.renderer;

import io.redspace.ironsspellbooks.entity.mobs.wizards.pyromancer.PyromancerRenderer;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.summoned.SummonedPyromancer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SummonedPyromancerRenderer extends PyromancerRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            More_iss.MODID,
            "textures/entity/summoned_pyromancer.png"
    );

    public SummonedPyromancerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public ResourceLocation getTextureLocation(SummonedPyromancer entity) {
        // 通常のPyromancerと同じテクスチャを使用（必要に応じて差し替え）
        return ResourceLocation.fromNamespaceAndPath(
                "irons_spellbooks",
                "textures/entity/wizard/pyromancer.png"
        );
    }
}