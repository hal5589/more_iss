package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class UnfadingAoe extends AoeEntity implements AntiMagicSusceptible {

    public UnfadingAoe(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public UnfadingAoe(Level level) {
        // Irons Spellbooks標準のHEALING_AOEを安全に再利用
        this(EntityRegistry.HEALING_AOE.get(), level);
    }

    @Override
    public void tick() {
        super.tick();

        // 🌟 バニラの仕組みで、常時バフの半透明緑モヤモヤ（ambient_entity_effect）を直接出す！
        if (this.level().isClientSide) {
            double radius = this.getRadius();
            for (int i = 0; i < 3; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double r = this.random.nextDouble() * radius;
                double pX = this.getX() + Math.cos(angle) * r;
                double pY = this.getY() + 0.2;
                double pZ = this.getZ() + Math.sin(angle) * r;

                // RGB指定：赤(0.2D), 緑(0.8D), 青(0.2D) = きれいな黄緑色
                this.level().addParticle(ParticleTypes.AMBIENT_ENTITY_EFFECT, pX, pY, pZ, 0.2D, 0.8D, 0.2D);
            }
        }
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (getOwner() instanceof LivingEntity owner && Utils.shouldHealEntity(owner, target)) {
            // 🟢 陣の中にいる味方全員の「毒」と「衰弱」を毎Tick強制解除
            if (target.hasEffect(MobEffects.POISON)) {
                target.removeEffect(MobEffects.POISON);
            }
            if (target.hasEffect(MobEffects.WITHER)) {
                target.removeEffect(MobEffects.WITHER);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return !pTarget.isSpectator() && pTarget.isAlive() && pTarget.isPickable();
    }

    @Override
    public float getParticleCount() {
        return 0f;
    }

    @Override
    protected float getParticleSpeedModifier() {
        return 0f;
    }

    @Override
    protected Vec3 getInflation() {
        return new Vec3(0, 1, 0);
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        // Ironsのシステムには何も渡さない（空っぽ）でOK
        return Optional.empty();
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        discard();
    }
}