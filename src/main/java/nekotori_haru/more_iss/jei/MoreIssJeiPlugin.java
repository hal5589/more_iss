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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1. 素材1：帯電 (charge) Lv3 のスクロール
        ItemStack chargeScroll = createScrollWithSpell(SpellRegistry.CHARGE_SPELL.get(), 3);

        // 2. 素材2：ハートストップ (heartstop) Lv5 のスクロール
        ItemStack heartstopScroll = createScrollWithSpell(SpellRegistry.HEARTSTOP_SPELL.get(), 5);

        // 3. 完成品：オーバーバースト・ブラッド (overburst_blood) Lv1 のスクロール
        ItemStack outputScroll = createScrollWithSpell(SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst_blood")), 1);

        // 3つの指定NBTスクロールが全て正常に生成されている場合のみ処理
        if (!chargeScroll.isEmpty() && !heartstopScroll.isEmpty() && !outputScroll.isEmpty()) {
            try {
                // 💡 ソースコードから判明した正しいレシピ型を指定
                mezz.jei.api.recipe.RecipeType arcaneAnvilType = new mezz.jei.api.recipe.RecipeType(
                        new ResourceLocation("irons_spellbooks", "arcane_anvil"),
                        Class.forName("io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe")
                );

                // 💡 ArcaneAnvilJeiRecipe のコンストラクタをリフレクションで取得
                // (恐らく引数は ItemStack, ItemStack, ItemStack の3つ、またはそれに準ずる並び)
                java.lang.reflect.Constructor<?> recipeConstructor = Class.forName("io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe")
                        .getDeclaredConstructor(ItemStack.class, ItemStack.class, ItemStack.class);
                recipeConstructor.setAccessible(true);

                // 正しいクラスでインスタンスを生成
                Object customAnvilRecipeInstance = recipeConstructor.newInstance(chargeScroll, heartstopScroll, outputScroll);

                // 秘術の金床専用カテゴリへレシピを追加
                registration.addRecipes(arcaneAnvilType, List.of(customAnvilRecipeInstance));
                System.out.println("[More ISS] 判明したArcaneAnvilJeiRecipe構造を用いて、秘術の金床カテゴリへ100%完璧な統合を完了しました。");

            } catch (Exception e) {
                // 万が一コンストラクタの引数の型（a, b, cを内包する独自レコード等）が違った場合の対策ログ
                System.err.println("[More ISS] コンストラクタの型が一致しませんでした。引数の構造を確認してください。");
                e.printStackTrace();
            }
        }
    }

    /**
     * 指定したスペルとレベルを保持したNBT付きのスクロールアイテム（ItemStack）を生成する共通メソッド
     */
    private ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
        if (spell == null || spell.getSpellId().equals("irons_spellbooks:none")) {
            return ItemStack.EMPTY;
        }

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