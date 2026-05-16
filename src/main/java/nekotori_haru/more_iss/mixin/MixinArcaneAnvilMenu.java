package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import nekotori_haru.more_iss.recipe.AnvilCraftRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public class MixinArcaneAnvilMenu {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void onCreateResult(CallbackInfo ci) {
        ArcaneAnvilMenu self = (ArcaneAnvilMenu) (Object) this;

        ItemStack baseItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(0);
        ItemStack modifierItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(1);

        if (baseItem.isEmpty() || modifierItem.isEmpty()) return;

        if (baseItem.getItem() instanceof Scroll && modifierItem.getItem() instanceof Scroll) {

            // NBTからスクロールのスペルIDを取得
            String id1 = getSpellIdFromScrollNbt(baseItem);
            String id2 = getSpellIdFromScrollNbt(modifierItem);

            if (id1.isEmpty() || id2.isEmpty()) return;

            // NBTからスクロールのスペルレベルを取得
            int lvl1 = getSpellLevelFromScrollNbt(baseItem);
            int lvl2 = getSpellLevelFromScrollNbt(modifierItem);

            Player player = ((ItemCombinerMenuAccessor) self).getPlayer();
            if (player == null) return;

            Level level = player.level();
            if (level == null) return;

            List<AnvilCraftRecipe> recipes = level.getRecipeManager().getAllRecipesFor(AnvilCraftRecipe.TYPE);

            for (AnvilCraftRecipe recipe : recipes) {
                // IDとレベルの両方がレシピの条件を満たしているかチェック
                if (recipe.matchesSpells(id1, lvl1, id2, lvl2)) {

                    AbstractSpell outputSpell = SpellRegistry.getSpell(new ResourceLocation(recipe.getOutputSpellId()));

                    if (outputSpell != null && !outputSpell.getSpellId().equals("irons_spellbooks:none")) {
                        ItemStack result = baseItem.copy();
                        result.setCount(1);

                        // JSONで指定された「出力レベル（recipe.getOutputLevel()）」でスクロールを生成
                        io.redspace.ironsspellbooks.api.spells.ISpellContainer.createScrollContainer(
                                outputSpell,
                                recipe.getOutputLevel(),
                                result
                        );

                        ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
                        break;
                    }
                }
            }
        }
    }

    /**
     * スクロールからスペルIDを抽出
     */
    private String getSpellIdFromScrollNbt(ItemStack stack) {
        CompoundTag spellTag = getFirstSpellTag(stack);
        if (spellTag != null && spellTag.contains("id", Tag.TAG_STRING)) {
            return spellTag.getString("id");
        }
        return "";
    }

    /**
     * スクロールからスペルレベルを抽出
     */
    private int getSpellLevelFromScrollNbt(ItemStack stack) {
        CompoundTag spellTag = getFirstSpellTag(stack);
        if (spellTag != null && spellTag.contains("level", Tag.TAG_ANY_NUMERIC)) {
            return spellTag.getInt("level");
        }
        return 1; // 見つからない場合のフォールバック
    }

    /**
     * ISpellContainerの最初のスペル用CompoundTagを返す共通ヘルパー
     */
    private CompoundTag getFirstSpellTag(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("ISpellContainer", Tag.TAG_COMPOUND)) {
            CompoundTag container = tag.getCompound("ISpellContainer");
            if (container.contains("spells", Tag.TAG_LIST)) {
                ListTag spellsList = container.getList("spells", Tag.TAG_COMPOUND);
                if (!spellsList.isEmpty()) {
                    return spellsList.getCompound(0);
                }
            }
        }
        return null;
    }
}