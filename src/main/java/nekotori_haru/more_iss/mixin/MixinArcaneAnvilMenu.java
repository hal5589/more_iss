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

        // アクセサーを介して入力スロットの ItemStack を取得
        ItemStack baseItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(0);
        ItemStack modifierItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(1);

        // どちらかのスロットが空なら処理しない
        if (baseItem.isEmpty() || modifierItem.isEmpty()) return;

        // 両方ともスクロールアイテムである場合のみ処理
        if (baseItem.getItem() instanceof Scroll && modifierItem.getItem() instanceof Scroll) {

            // NBTからスペルID（文字列）を直接取得
            String id1 = getSpellIdFromScrollNbt(baseItem);
            String id2 = getSpellIdFromScrollNbt(modifierItem);

            // スペルIDが正常に取得できない場合は終了
            if (id1.isEmpty() || id2.isEmpty()) return;

            // アクセサー経由で安全にプレイヤーとワールドインスタンスを取得
            Player player = ((ItemCombinerMenuAccessor) self).getPlayer();
            if (player == null) return;

            Level level = player.level();
            if (level == null) return;

            // データパック（JSON）から登録されたカスタムレシピをすべて取得
            List<AnvilCraftRecipe> recipes = level.getRecipeManager().getAllRecipesFor(AnvilCraftRecipe.TYPE);

            // 条件（スペルIDの組み合わせ）に合致するレシピがあるか探索
            for (AnvilCraftRecipe recipe : recipes) {
                if (recipe.matchesSpells(id1, id2)) {

                    // JSONに指定されている出力先スペルを取得
                    AbstractSpell outputSpell = SpellRegistry.getSpell(new ResourceLocation(recipe.getOutputSpellId()));

                    // 出力スペルが正常に存在している場合のみ結果を生成（NoneSpellによるクラッシュ防止）
                    if (outputSpell != null && !outputSpell.getSpellId().equals("irons_spellbooks:none")) {
                        ItemStack result = baseItem.copy();
                        result.setCount(1);

                        // 結果用スクロールに新しいスペルを書き込む
                        io.redspace.ironsspellbooks.api.spells.ISpellContainer.createScrollContainer(outputSpell, 1, result);

                        // 金床の出力スロットにセット
                        ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
                        break; // レシピが適用されたらループを終了
                    }
                }
            }
        }
    }

    /**
     * スクロールの ItemStack から NBT を直接読み取り、最初のスペルIDを文字列で返します
     */
    private String getSpellIdFromScrollNbt(ItemStack stack) {
        if (!stack.hasTag()) return "";

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("ISpellContainer", Tag.TAG_COMPOUND)) {
            CompoundTag container = tag.getCompound("ISpellContainer");

            if (container.contains("spells", Tag.TAG_LIST)) {
                ListTag spellsList = container.getList("spells", Tag.TAG_COMPOUND);

                if (!spellsList.isEmpty()) {
                    CompoundTag firstSpell = spellsList.getCompound(0);
                    if (firstSpell.contains("id", Tag.TAG_STRING)) {
                        return firstSpell.getString("id");
                    }
                }
            }
        }
        return "";
    }
}