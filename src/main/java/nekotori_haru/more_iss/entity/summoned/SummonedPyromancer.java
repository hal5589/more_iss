package nekotori_haru.more_iss.entity.summoned;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.pyromancer.PyromancerEntity;
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
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class SummonedPyromancer extends PyromancerEntity implements IMagicSummon {

    private LivingEntity owner;
    private String spellId;  // String 型に変更

    public SummonedPyromancer(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        this.setCustomName(Component.translatable("entity.more_iss.summoned_pyromancer"));
        this.setCustomNameVisible(false);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PYROMANCER_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.PYROMANCER_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Deprecated(forRemoval = true)
    public SummonedPyromancer(Level level, LivingEntity owner) {
        this(EntityRegistry.PYROMANCER.get(), level);
        setSummoner(owner);
    }

    // ⭐ String を受け取る
    public void setSpellId(String spellId) {
        this.spellId = spellId;
    }

    // ⭐ String を返す
    public String getSpellId() {
        return this.spellId;
    }

    @Override
    public void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WizardAttackGoal(this, 1.25f, 25, 50)
                .setSpells(
                        List.of(SpellRegistry.FIREBOLT_SPELL.get(), SpellRegistry.FIREBOLT_SPELL.get(), SpellRegistry.FIREBOLT_SPELL.get(), SpellRegistry.FIRE_BREATH_SPELL.get(), SpellRegistry.BLAZE_STORM_SPELL.get()),
                        List.of(),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get()),
                        List.of()
                )
                .setSingleUseSpell(SpellRegistry.MAGMA_BOMB_SPELL.get(), 80, 200, 4, 6)
        );
        this.goalSelector.addGoal(2, new GenericFollowOwnerGoal(this, this::getSummoner, 0.9f, 15, 5, false, 25));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Mob.class, 8.0F));
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
        // ⭐ SummonManager.removeSummon() を呼んで recast 削除を自動化
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
