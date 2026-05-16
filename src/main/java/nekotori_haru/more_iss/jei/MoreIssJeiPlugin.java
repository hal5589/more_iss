package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
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
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 💡 自作した専用カテゴリ（秘術の金床の見た目をしたスロット）をJEIに登録
        registration.addRecipeCategories(new MoreIssAnvilCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack chargeScroll = createScrollWithSpell(SpellRegistry.CHARGE_SPELL.get(), 3);
        ItemStack heartstopScroll = createScrollWithSpell(SpellRegistry.HEARTSTOP_SPELL.get(), 5);
        ItemStack outputScroll = createScrollWithSpell(SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst_blood")), 1);

        if (!chargeScroll.isEmpty() && !heartstopScroll.isEmpty() && !outputScroll.isEmpty()) {
            // バニラの金床のデータ構造（左、右、出力）をそのまま流用してシリアライズ
            IJeiAnvilRecipe myCustomAnvilRecipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    chargeScroll,
                    List.of(heartstopScroll),
                    List.of(outputScroll)
            );

            // 💡 バニラ金床ではなく、自作した「MoreIssAnvilCategory.TYPE」にレシピを流し込む！
            registration.addRecipes(MoreIssAnvilCategory.TYPE, List.of(myCustomAnvilRecipe));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 💡 自作カテゴリの「唯一のアイコン（触媒）」として、秘術の金床ブロックを登録
        net.minecraft.world.item.Item arcaneAnvilItem = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("irons_spellbooks", "arcane_anvil")
        );
        if (arcaneAnvilItem != null && arcaneAnvilItem != Items.AIR) {
            registration.addRecipeCatalyst(new ItemStack(arcaneAnvilItem), MoreIssAnvilCategory.TYPE);
        }
    }

    private ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
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