package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// あなたのModのメインクラスが「More_iss」または「MoreIssMod」等、環境に合わせて適宜インポートしてください
// import nekotori_haru.more_iss.More_iss;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public class MixinArcaneAnvilMenu_overburst {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void onCreateResult(CallbackInfo ci) {
        ArcaneAnvilMenu self = (ArcaneAnvilMenu) (Object) this;

        // アクセサーを介して入力スロットの ItemStack を取得
        ItemStack baseItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(0);
        ItemStack modifierItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(1);

        if (baseItem.isEmpty() || modifierItem.isEmpty()) return;

        if (baseItem.getItem() instanceof Scroll && modifierItem.getItem() instanceof Scroll) {
            SpellData spell1 = ISpellContainer.get(baseItem).getSpellAtIndex(0);
            SpellData spell2 = ISpellContainer.get(modifierItem).getSpellAtIndex(0);

            if (spell1 == null || spell2 == null) return;

            // 最初のスロットがElectrocuteで、隣のスロットがHeartstopの場合
            if (spell1.getSpell().equals(SpellRegistry.ELECTROCUTE_SPELL.get()) &&
                    spell2.getSpell().equals(SpellRegistry.HEARTSTOP_SPELL.get())) {

                // 【レベル判定】
                // 「==」ならピンポイント指定、「>=」にすればそのレベル以上で合成可能になります
                if (spell1.getLevel() == 3 || spell2.getLevel() == 5) {
                    return; // 条件を満たしていない場合は弾く
                }

                ItemStack result = baseItem.copy();
                result.setCount(1);

                // あなたのModのカスタムスペル（レベル1）をスクロールに上書き生成
                // ※ More_iss.OVERBURST_BLOOD.get() の登録名に合わせて適宜書き換えてください
                ISpellContainer.createScrollContainer(More_iss.OVERBURST_BLOOD.get(), 1, result);

                // アクセサーを介して出力スロットへセット
                ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
            }
        }
    }
}