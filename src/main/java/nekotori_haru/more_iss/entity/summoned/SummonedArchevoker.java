package nekotori_haru.more_iss.entity.summoned;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.archevoker.ArchevokerEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class SummonedArchevoker extends ArchevokerEntity implements IMagicSummon {

    private LivingEntity owner;

    public SummonedArchevoker(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        this.setCustomName(Component.translatable("entity.more_iss.summoned_archevoker"));
        this.setCustomNameVisible(false);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.ARCHEVOKER_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ARCHEVOKER_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Deprecated(forRemoval = true)
    public SummonedArchevoker(Level level, LivingEntity owner) {
        this(EntityRegistry.ARCHEVOKER.get(), level);
        setSummoner(owner);
    }

    @Override
    public void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpellBarrageGoal(this, SpellRegistry.SUMMON_VEX_SPELL.get(), 1, 3, 100, 260, 1));
        this.goalSelector.addGoal(1, new GustDefenseGoal(this));
        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.5f, 30, 80)
                .setSpells(
                        List.of(SpellRegistry.FANG_STRIKE_SPELL.get(), SpellRegistry.FIRECRACKER_SPELL.get()),
                        List.of(SpellRegistry.FANG_WARD_SPELL.get(), SpellRegistry.SHIELD_SPELL.get()),
                        List.of(),
                        List.of())
                .setSpellQuality(.4f, .6f)
                .setSingleUseSpell(SpellRegistry.INVISIBILITY_SPELL.get(), 40, 80, 5, 5)
                .setDrinksPotions());
        this.goalSelector.addGoal(3, new GenericFollowOwnerGoal(this, this::getSummoner, 0.9f, 15, 5, false, 25));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
    }

    @Override
    public LivingEntity getSummoner() {
        return this.owner;
    }

    @Deprecated(forRemoval = true)
    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner == null) return;
        this.owner = owner;
        SummonManager.setOwner(this, owner);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            LivingEntity owner = getSummoner();
            if (owner != null) {
                if (owner instanceof Mob ownerMob) {
                    LivingEntity ownerTarget = ownerMob.getTarget();
                    if (ownerTarget != null && ownerTarget.isAlive()) {
                        if (this.getTarget() == null || this.getTarget() != ownerTarget) {
                            this.setTarget(ownerTarget);
                        }
                    }
                }
                LivingEntity lastHurtBy = owner.getLastHurtByMob();
                if (lastHurtBy != null && lastHurtBy.isAlive()) {
                    if (this.getTarget() == null || this.getTarget() != lastHurtBy) {
                        this.setTarget(lastHurtBy);
                    }
                }
            }
        }
    }

    @Override
    public int getExperienceReward() {
        return 0;
    }

    @Override
    public void die(DamageSource pDamageSource) {
        if (!level().isClientSide) {
            SummonManager.removeSummon(this);
        }
        this.onDeathHelper();
        this.deathTime = 20;
        this.remove(RemovalReason.KILLED);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (shouldIgnoreDamage(pSource))
            return false;
        if (!level().isClientSide && pSource.getEntity() instanceof LivingEntity attacker) {
            LivingEntity owner = getSummoner();
            if (owner != null && attacker != owner) {
                this.setTarget(attacker);
            }
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.onRemovedHelper(this);
        super.remove(reason);
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void onUnSummon() {
        if (!level().isClientSide) {
            MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
            this.discard();
        }
    }
}