package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@JeiPlugin
public class MoreIssJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("more_iss", "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1. 素材：感電 Lv10
        ItemStack electrocuteScroll = createScrollWithSpell(
                SpellRegistry.ELECTROCUTE_SPELL.get(), 10
        );

        // 2. 素材：ハートストップ Lv5
        ItemStack heartstopScroll = createScrollWithSpell(
                SpellRegistry.HEARTSTOP_SPELL.get(), 5
        );

        // 3. 完成品：overburst Lv1
        AbstractSpell overburstSpell = SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst"));
        ItemStack outputScroll = ItemStack.EMPTY;

        if (overburstSpell != null && !overburstSpell.getSpellId().equals("irons_spellbooks:none")) {
            outputScroll = createScrollWithSpell(overburstSpell, 1);
        }

        // すべてのアイテムが正常に準備できている場合のみ処理
        if (!electrocuteScroll.isEmpty() && !heartstopScroll.isEmpty() && !outputScroll.isEmpty()) {

            // 💡 秘術の金床のレシピタイプをJEI標準のオブジェクトとして直接定義
            // これにより、ISS側の内部クラス（AnvilRecipeWrapper）への依存を完全に無くします
            mezz.jei.api.recipe.RecipeType<Object> arcaneAnvilType =
                    new mezz.jei.api.recipe.RecipeType<>(
                            new ResourceLocation("irons_spellbooks", "arcane_anvil"),
                            Object.class
                    );

            // JEIバニラのファクトリーを使って、金床形式のデータ（左、右、出力）を生成
            Object vanillaAnvilRecipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    electrocuteScroll,
                    List.of(heartstopScroll),
                    List.of(outputScroll)
            );

            // 4. カテゴリIDだけ「秘術の金床」を指定してレシピを追加
            registration.addRecipes(arcaneAnvilType, List.of(vanillaAnvilRecipe));
        }
    }

    private ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
        net.minecraft.world.item.Item scrollItem = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("irons_spellbooks", "scroll")
        );

        if (scrollItem == null || scrollItem == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(scrollItem, 1);
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }
}