package nekotori_haru.more_iss.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class MoreIssJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation("more_iss", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /**
     * 独自の魔法合成カテゴリ（秘術の金床画面の枠組み）を登録
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MoreIssAnvilCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * 画像にあるすべての魔法レシピクラスをここで一括で呼び出して登録する
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1. FREISCHUTZ (画像通り MoreIssAnvilCategory_FREISCHUTZ)
        MoreIssAnvilCategory_FREISCHUTZ.register(registration);

        // 2. OVERBURST (💡 ここを画像に合わせて MoreIss に修正)
        MoreIssAnvilCategory_OVERBURST.register(registration);

        // 3. SACRIFICIAL_EDGE (💡 ここも画像に合わせて MoreIss に修正)
        MoreIssAnvilCategory_SACRIFICIAL_EDGE.register(registration);
    }
}