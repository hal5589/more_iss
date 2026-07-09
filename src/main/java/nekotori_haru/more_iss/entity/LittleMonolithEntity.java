package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class LittleMonolithEntity extends LivingEntity {

    private static final EntityDataAccessor<Integer> DATA_SPELL_LEVEL =
            SynchedEntityData.defineId(LittleMonolithEntity.class, EntityDataSerializers.INT);

    private LivingEntity owner;

    // ============================================================
    //  コンストラクタ
    // ============================================================

    public LittleMonolithEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(false);
    }

    public static LittleMonolithEntity create(Level level, LivingEntity owner, int spellLevel, float health) {
        LittleMonolithEntity entity = new LittleMonolithEntity(ModEntities.LITTLE_MONOLITH.get(), level);
        entity.owner = owner;
        entity.setSpellLevel(spellLevel);

        // 最大体力を動的に設定
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        entity.setHealth(health);

        return entity;
    }

    // ============================================================
    //  属性登録
    // ============================================================

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    // ============================================================
    //  データ管理
    // ============================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SPELL_LEVEL, 1);
    }

    public int getSpellLevel() {
        return this.entityData.get(DATA_SPELL_LEVEL);
    }

    public void setSpellLevel(int level) {
        this.entityData.set(DATA_SPELL_LEVEL, level);
    }

    // ============================================================
    //  回復の完全無効化
    // ============================================================

    @Override
    public void heal(float amount) {
        // 回復を一切受け付けない
    }

    // ============================================================
    //  更新処理（制限時間なし）
    // ============================================================

    @Override
    public void tick() {
        super.tick();
        // ★ 制限時間による削除処理を完全に削除
        // 体力が0になったときのみ削除（super.tick 内で処理される）
    }

    // ============================================================
    //  ダメージ処理（プレイヤー攻撃で範囲爆発）
    // ============================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;

        // ★ モノリス自身の無敵時間を常に0にリセット
        this.invulnerableTime = 0;

        boolean isPlayerSource = source.getEntity() instanceof Player;
        float healthBefore = this.getHealth();
        boolean result = super.hurt(source, amount);

        // ノックバックを完全に無効化
        this.setDeltaMovement(Vec3.ZERO);

        // ★ 自身の無敵時間を再度リセット（念のため）
        this.invulnerableTime = 0;

        if (result && isPlayerSource) {
            // ★ ワンパン処理を無視：常に実際に受けたダメージを爆発ダメージとする
            float damageDealt = healthBefore - this.getHealth();
            // ダメージが0より大きい場合のみ爆発（0の場合は爆発不要）
            if (damageDealt > 0) {
                triggerExplosion((Player) source.getEntity(), damageDealt);
            }
        }

        return result;
    }

    private void triggerExplosion(Player attacker, float damageTaken) {
        Level level = this.level();
        if (level.isClientSide) return;

        int spellLevel = getSpellLevel();
        float radius = 6 + spellLevel * 0.75f;
        float damage = damageTaken;

        Vec3 center = this.position().add(0, 1, 0);

        // 1. 白色の衝撃波
        Vector3f whiteColor = new Vector3f(1.0f, 1.0f, 1.0f);
        MagicManager.spawnParticles(level, new BlastwaveParticleOptions(whiteColor, radius),
                center.x, center.y, center.z, 1, 0, 0, 0, 0, true);

        // 2. エンドロッドパーティクルを周囲に飛ばす
        for (int i = 0; i < 60; i++) {
            Vec3 offset = new Vec3(
                    (level.random.nextDouble() - 0.5) * radius * 2,
                    (level.random.nextDouble() - 0.5) * radius * 2,
                    (level.random.nextDouble() - 0.5) * radius * 2
            ).normalize().scale(radius * 0.8 + level.random.nextDouble() * radius * 0.4);
            Vec3 pos = center.add(offset);
            MagicManager.spawnParticles(level, ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, false);
        }

        // 3. 範囲内のエンティティにダメージ（攻撃者、自身、他のモノリスは除外）
        AABB aabb = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != attacker
                        && !e.getUUID().equals(this.getUUID())
                        && !(e instanceof LittleMonolithEntity) // 他のモノリスを除外
                        && !DamageSources.isFriendlyFireBetween(e, attacker)
                        && Utils.hasLineOfSight(level, this, e, true));

        for (LivingEntity target : entities) {
            if (target.distanceToSqr(center) > radius * radius) continue;

            // 無敵時間をリセットしてからダメージを与える（無敵時間無視）
            target.invulnerableTime = 0;
            target.hurt(attacker.damageSources().playerAttack(attacker), damage);

            MagicManager.spawnParticles(level, ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05, false);
        }

        // 4. モノリス自身の発動エフェクト
        MagicManager.spawnParticles(level, ParticleTypes.END_ROD,
                center.x, center.y, center.z, 20, 0.5, 0.5, 0.5, 0.1, false);
    }

    // ============================================================
    //  LivingEntity の抽象メソッド実装（ダミー）
    // ============================================================

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public @NotNull ItemStack getMainHandItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getOffhandItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // 何もしない
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public @NotNull ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    // ============================================================
    //  NBT 保存／読み込み
    // ============================================================

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpellLevel")) setSpellLevel(tag.getInt("SpellLevel"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpellLevel", getSpellLevel());
    }

    // ============================================================
    //  物理特性
    // ============================================================

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(0.8f, 2.0f);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }
}