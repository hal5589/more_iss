package nekotori_haru.more_iss.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DetonationEffect extends MobEffect {

    public DetonationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Level world = entity.level();
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            serverWorld.sendParticles(
                    ParticleTypes.LAVA,
                    entity.getRandomX(0.6D),
                    entity.getRandomY(),
                    entity.getRandomZ(0.6D),
                    1, 0, 0, 0, 0
            );
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}