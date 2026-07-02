package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.spell.ice.CryoConvergenceSpell;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = More_iss.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CryoConvergenceTicker {

    private static final String SPELL_ID = More_iss.MODID + ":cryo_convergence";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // サーバー側でのみ処理
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // MagicDataを取得して、このspellを詠唱中かチェック
        MagicData magicData = MagicData.getPlayerMagicData(entity);
        if (!magicData.isCasting()) return;

        String castingId = magicData.getCastingSpellId();
        if (!SPELL_ID.equals(castingId)) return;

        // ---- 詠唱中パーティクル ----
        RandomSource random = entity.getRandom();
        Vec3 lookVec = entity.getLookAngle();
        Vec3 center = entity.getEyePosition().add(lookVec.scale(1.2));

        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(lookVec.dot(up)) > 0.9) up = new Vec3(1, 0, 0);
        Vec3 right = lookVec.cross(up).normalize();
        Vec3 localUp = right.cross(lookVec).normalize();

        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double radius = 0.1 + random.nextDouble() * 0.8;
            double theta = random.nextDouble() * 2 * Math.PI;
            double depthOffset = (random.nextDouble() - 0.5) * 0.3;

            Vec3 offset = right.scale(radius * Math.cos(theta))
                    .add(localUp.scale(radius * Math.sin(theta)))
                    .add(lookVec.scale(depthOffset));

            Vec3 pos = center.add(offset);
            Vec3 vel = center.subtract(pos).normalize().scale(0.1 + random.nextDouble() * 0.1);
            vel = vel.add(0, 0.01, 0);

            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.x, pos.y, pos.z, 0,
                    vel.x, vel.y, vel.z, 0.5);

            if (random.nextDouble() < 0.1) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 0,
                        vel.x * 0.5, vel.y * 0.5, vel.z * 0.5, 0.3);
            }
        }
    }
}