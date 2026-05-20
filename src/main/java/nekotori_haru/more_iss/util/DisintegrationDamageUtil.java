package nekotori_haru.more_iss.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class DisintegrationDamageUtil {

    public static void dealTrueDamage(LivingEntity target, DamageSource source, float amount, boolean bypassInvul) {
        if (bypassInvul) {
            target.invulnerableTime = 0;
            target.setInvulnerable(false);
        }
        target.hurt(source, amount);
    }

    public static void applyForcedHealth(LivingEntity entity, float health) {
        try {
            entity.setHealth(health);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}