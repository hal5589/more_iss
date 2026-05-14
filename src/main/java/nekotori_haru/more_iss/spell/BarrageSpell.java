package nekotori_haru.more_iss.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;  // ← これが抜けていた
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class BarrageSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath("more_iss", "barrage");

    // 本家と同じ DefaultConfig の書き方
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public BarrageSpell() {
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 250;
        this.manaCostPerLevel = 20;
        this.castTime = 1;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal("Projectiles: " + getBulletCount(spellLevel)),
                Component.literal("Spread: " + (int) SPREAD_DEG + "°")
        );
    }

    // -------------------------------------------------------
    // 弾幕パラメータ
    // -------------------------------------------------------

    /** Lv1〜5の弾数 */
    private static final int[] BULLET_COUNT = { 10, 15, 20, 25, 30 };

    /** 水平拡散角度（度）*/
    private static final float SPREAD_DEG = 20f;

    /** 縦方向の揺らぎ（度）。0 にすると水平一列 */
    private static final float V_SPREAD_DEG = 3f;

    // -------------------------------------------------------
    // キャスト処理
    // -------------------------------------------------------

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity,
                       CastSource castSource, MagicData playerMagicData) {

        int   count  = getBulletCount(spellLevel);
        float damage = getDamage(spellLevel, entity);
        Vec3  look   = entity.getLookAngle().normalize();

        // 発射起点：本家に合わせて目線の高さから
        Vec3 origin = entity.position().add(0,
                entity.getEyeHeight() - new MagicMissileProjectile(world, entity)
                        .getBoundingBox().getYsize() * .5f, 0);

        if (count == 1) {
            spawnMissile(world, entity, origin, look, damage);
        } else {
            float half = SPREAD_DEG / 2f;
            float step = SPREAD_DEG / (count - 1);

            for (int i = 0; i < count; i++) {
                float yaw   = -half + step * i;
                float pitch = V_SPREAD_DEG > 0
                        ? (float)(Math.random() * V_SPREAD_DEG * 2 - V_SPREAD_DEG)
                        : 0f;
                Vec3 dir = rotateDirection(look, yaw, pitch);
                spawnMissile(world, entity, origin, dir, damage);
            }
        }

        // 本家と同じく super を最後に呼ぶ
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    // -------------------------------------------------------
    // 飛翔体の生成（本家 MagicMissileProjectile をそのまま流用）
    // -------------------------------------------------------

    private void spawnMissile(Level world, LivingEntity caster,
                              Vec3 origin, Vec3 direction, float damage) {
        MagicMissileProjectile missile = new MagicMissileProjectile(world, caster);
        missile.setPos(origin.x, origin.y, origin.z);
        missile.shoot(direction);   // Vec3 を受け取る shoot を使う
        missile.setDamage(damage);
        world.addFreshEntity(missile);
    }

    // -------------------------------------------------------
    // サウンド（任意）
    // -------------------------------------------------------

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDER_EYE_LAUNCH);
    }

    // -------------------------------------------------------
    // ユーティリティ
    // -------------------------------------------------------

    private int getBulletCount(int level) {
        return BULLET_COUNT[Math.min(level - 1, BULLET_COUNT.length - 1)];
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) * .5f;
    }

    /**
     * 基準方向ベクトルをヨー（水平）・ピッチ（垂直）で回転させる。
     */
    private static Vec3 rotateDirection(Vec3 base, float yawDeg, float pitchDeg) {
        double yaw  = Math.toRadians(yawDeg);
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double x1 = base.x * cosY + base.z * sinY;
        double y1 = base.y;
        double z1 = -base.x * sinY + base.z * cosY;

        double pitch = Math.toRadians(pitchDeg);
        double hLen  = Math.sqrt(x1 * x1 + z1 * z1);
        double cosP  = Math.cos(pitch), sinP = Math.sin(pitch);
        double x2, y2, z2;
        if (hLen < 1e-6) {
            x2 = x1; y2 = y1; z2 = z1;
        } else {
            double newH = hLen * cosP - y1 * sinP;
            y2 = hLen * sinP + y1 * cosP;
            x2 = x1 * (newH / hLen);
            z2 = z1 * (newH / hLen);
        }
        return new Vec3(x2, y2, z2).normalize();
    }
}
