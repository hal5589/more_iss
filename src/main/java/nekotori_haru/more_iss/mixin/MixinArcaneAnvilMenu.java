package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import nekotori_haru.more_iss.recipe.AnvilCraftRecipe;
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

        // アクセサー経由で2つの入力スロットからItemStackを取得
        ItemStack baseItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(0);
        ItemStack modifierItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(1);

        // どちらかが空なら何もしない
        if (baseItem.isEmpty() || modifierItem.isEmpty()) return;

        // アクセサー経由でプレイヤーとワールドを安全に取得
        Player player = ((ItemCombinerMenuAccessor) self).getPlayer();
        if (player == null) return;

        Level level = player.level();
        if (level == null) return;

        // データパックから "more_iss:anvil_craft" の全カスタムレシピを取得
        List<AnvilCraftRecipe> recipes = level.getRecipeManager().getAllRecipesFor(AnvilCraftRecipe.TYPE);

        // レシピ群をループし、アイテムID & NBT の組み合わせが一致するものを探す
        for (AnvilCraftRecipe recipe : recipes) {
            if (recipe.matchesItems(baseItem, modifierItem)) {

                // レシピから設定された出力用のItemStack（NBT付き）のコピーを生成
                ItemStack resultStack = recipe.getOutputCopy();

                if (!resultStack.isEmpty()) {
                    // 金床の出力スロット（0番）にセット
                    ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, resultStack);
                    break; // 合致するレシピが見つかったためループを終了
                }
            }
        }
    }
}