package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.registration.IRecipeRegistration;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MoreIssAnvilCategory_SACRIFICIAL_EDGE {

    public static void register(IRecipeRegistration registration) {

        // ─── 💡 ここであなたの好きなようにItemStackを100%自由に指定できます！ ───

        // 左スロット: サクリファイス Lv3
        ItemStack leftInput = createScrollWithSpell(SpellRegistry.SACRIFICE_SPELL.get(), 3);

        // 右スロット: ブラッドスラッシュ Lv3
        ItemStack rightInput = createScrollWithSpell(SpellRegistry.BLOOD_SLASH_SPELL.get(), 3);

        // 出力スロット: サクリファシアル・エッジ Lv1
        ItemStack output = createScrollWithSpell(More_iss.SACRIFICIAL_EDGE.get(), 1);

        // すべてのアイテムが正常に生成されている場合のみJEIへ登録
        if (!leftInput.isEmpty() && !rightInput.isEmpty() && !output.isEmpty()) {
            IJeiAnvilRecipe recipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    leftInput,
                    List.of(rightInput),
                    List.of(output)
            );

            // 💡 正しい共通カテゴリ「MoreIssAnvilCategory.TYPE」へ流し込み
            registration.addRecipes(MoreIssAnvilCategory.TYPE, List.of(recipe));
        }
    }

    // スクロールを確実に生成する安全なメソッド
    private static ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
        if (spell == null || spell.getSpellId().equals("irons_spellbooks:none")) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "scroll"));
        if (scrollItem == null || scrollItem == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(scrollItem, 1);
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }
}