package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public class MixinArcaneAnvilMenu {

    // 別のModや自作のカスタムスペルなら、"irons_spellbooks" の部分をそのModのModIDに書き換えてください
    private static final AbstractSpell BEAM = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", "annihilation_beam"));
    private static final AbstractSpell BOMB = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", "annihilation_bomb"));
    private static final AbstractSpell THORNS = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", "ambush_thorns"));
    private static final AbstractSpell RESONANCE = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", "annihilation_resonance"));

    // 合成結果として出てくるカスタムスペル（more_iss:overburst_bloodなど、あなたのスペルIDを指定）
    private static final AbstractSpell GEYSER = SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst_blood"));

    @Inject(method = "createResult", at = @At("TAIL"))
    private void onCreateResult(CallbackInfo ci) {
        ArcaneAnvilMenu self = (ArcaneAnvilMenu) (Object) this;

        ItemStack baseItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(0);
        ItemStack modifierItem = ((ItemCombinerMenuAccessor) self).getInputSlots().getItem(1);

        if (baseItem.isEmpty() || modifierItem.isEmpty()) return;

        // 順番固定版
        if (baseItem.getItem() instanceof Scroll && modifierItem.getItem() instanceof Scroll) {
            SpellData spell1 = ISpellContainer.get(baseItem).getSpellAtIndex(0);
            SpellData spell2 = ISpellContainer.get(modifierItem).getSpellAtIndex(0);

            if (spell1 != null && spell2 != null) {
                // 事前に定義したスペルオブジェクトと直接比較します
                if (spell1.getSpell().equals(BEAM) && spell2.getSpell().equals(BOMB)) {
                    if (GEYSER == null) return; // 安全対策

                    ItemStack result = baseItem.copy();
                    result.setCount(1);
                    ISpellContainer.createScrollContainer(GEYSER, 1, result);
                    ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
                    return; // マッチしたらここで終了
                }
            }
        }

        // 順番関係ない版
        if (baseItem.getItem() instanceof Scroll && modifierItem.getItem() instanceof Scroll) {
            SpellData spell1 = ISpellContainer.get(baseItem).getSpellAtIndex(0);
            SpellData spell2 = ISpellContainer.get(modifierItem).getSpellAtIndex(0);

            if (spell1 != null && spell2 != null) {
                Set<AbstractSpell> spells = Set.of(spell1.getSpell(), spell2.getSpell());

                // 定義した2つのスペルが含まれているか Set でスマートに判定
                if (BEAM != null && BOMB != null && spells.equals(Set.of(THORNS, RESONANCE))) {
                    if (GEYSER == null) return;

                    ItemStack result = baseItem.copy();
                    result.setCount(1);
                    ISpellContainer.createScrollContainer(GEYSER, 1, result);
                    ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
                }
            }
        }
    }
}