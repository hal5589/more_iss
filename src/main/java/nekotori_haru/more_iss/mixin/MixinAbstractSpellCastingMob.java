package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mob側（EternalWizardEntity等）のキャスト開始経路をブロックするMixin。
 * AbstractSpellCastingMob#initiateCastSpell はプレイヤーの attemptInitiateCast とは
 * 完全に別の入口なので、こちらにも同様のOBLIVION判定を差し込む必要がある。
 */
@Mixin(value = AbstractSpellCastingMob.class, remap = false)
public abstract class MixinAbstractSpellCastingMob {

    @Inject(
            method = "initiateCastSpell(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInitiateCastSpell(AbstractSpell spell, int spellLevel, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ModEffects.OBLIVION != null && self.hasEffect(ModEffects.OBLIVION.get())) {
            ci.cancel();
        }
    }
}