package nekotori_haru.more_iss.entity.summoned;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.priest.PriestEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class SummonedPriest extends PriestEntity implements IMagicSummon {

    private LivingEntity owner;

    public SummonedPriest(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        this.setCustomName(Component.translatable("entity.more_iss.summoned_priest"));
        this.setCustomNameVisible(false);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PRIEST_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.PRIEST_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Deprecated(forRemoval = true)
    public SummonedPriest(Level level, LivingEntity owner) {
        this(EntityRegistry.PRIEST.get(), level);
        setSummoner(owner);
    }

    @Override
    public void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new GustDefenseGoal(this));
        this.goalSelector.addGoal(2, new WizardSupportGoal<>(this, 1.25f, 100, 180)
                .setSpells(
                        List.of(SpellRegistry.BLESSING_OF_LIFE_SPELL.get(), SpellRegistry.BLESSING_OF_LIFE_SPELL.get(), SpellRegistry.HEALING_CIRCLE_SPELL.get()),
                        List.of(SpellRegistry.FORTIFY_SPELL.get())
                ));
        this.goalSelector.addGoal(3, new WizardAttackGoal(this, 1.25f, 35, 70)
                .setSpells(
                        List.of(SpellRegistry.WISP_SPELL.get(), SpellRegistry.GUIDING_BOLT_SPELL.get()),
                        List.of(SpellRegistry.GUST_SPELL.get()),
                        List.of(),
                        List.of(SpellRegistry.HEAL_SPELL.get()))
                .setSpellQuality(0.3f, 0.5f)
                .setDrinksPotions());
        this.goalSelector.addGoal(4, new GenericFollowOwnerGoal(this, this::getSummoner, 0.9f, 15, 5, false, 25));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        this.supportTargetSelector = new GoalSelector(this.level().getProfilerSupplier());
        this.supportTargetSelector.addGoal(0, new FindSupportableTargetGoal<>(this, LivingEntity.class, true,
                (mob) -> !isAngryAt(mob) && mob.getHealth() * 1.25f < mob.getMaxHealth() && (mob.getType().is(ModTags.VILLAGE_ALLIES) || mob instanceof Player))
        );
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
    public MerchantOffers getOffers() {
        return new MerchantOffers();
    }

    @Override
    public boolean isTrading() {
        return false;
    }

    @Override
    public void openTradingScreen(Player player, Component displayName, int level) {
    }

    @Override
    public void stopTrading() {
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