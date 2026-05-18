package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MoreIssAnvilCategory_OVERBURST {

    /**
     * 💡 解決策A: 他のクラスと名前・構造を完全に統一
     */
    public static void register(IRecipeRegistration registration) {

        // ─── 1. 左スロット：チャージ Lv3 ───
        ItemStack leftInput = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.CHARGE_SPELL.get(), 3, leftInput);

        // ─── 2. 右スロット：ハートストップ Lv5 ───
        ItemStack rightInput = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.HEARTSTOP_SPELL.get(), 5, rightInput);

        // ─── 3. 出力スロット：オーバーバーストブラッド Lv1 ───
        ItemStack output = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(
                SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst_blood")), 1, output
        );

        // すべてのアイテムが正常に生成されている場合のみJEIへ登録
        if (!leftInput.isEmpty() && !rightInput.isEmpty() && !output.isEmpty()) {
            IJeiAnvilRecipe recipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    leftInput,
                    List.of(rightInput),
                    List.of(output)
            );

            // 💡 共通カテゴリ「MoreIssAnvilCategory.TYPE」へ直接流し込み
            registration.addRecipes(MoreIssAnvilCategory.TYPE, List.of(recipe));
        }
    }
}