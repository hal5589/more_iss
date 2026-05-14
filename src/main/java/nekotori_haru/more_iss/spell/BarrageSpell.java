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
            ResourceLocation.fromNamespaceAndPath("more_iss", "barrage_spell");

    // 本家と同じ DefaultConfig の書き方
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public BarrageSpell() {
        this.baseSpellPower = 25;
        this.spellPowerPerLevel = 5;
        this.baseManaCost = 300;
        this.manaCostPerLevel = 50;
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
                // 角度ではなく、長方形のサイズを表示するように変更
                Component.literal(String.format("Area: %.1fx%.1f", RECT_WIDTH, RECT_HEIGHT))
        );
    }

    // -------------------------------------------------------
    // 弾幕パラメータ
    // -------------------------------------------------------

    /** Lv1〜5の弾数 */
    private static final int[] BULLET_COUNT = { 30, 37, 45, 53, 60 };

    /** 長方形の横幅（ブロック数） */
    private static final float RECT_WIDTH = 15.0f;

    /** 長方形の縦幅（ブロック数） */
    private static final float RECT_HEIGHT = 5.0f;

    /** 背後へ下げる距離（ブロック数） */
    private static final float BACK_DISTANCE = 7f;

    // -------------------------------------------------------
    // キャスト処理
    // -------------------------------------------------------

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity,
                       CastSource castSource, MagicData playerMagicData) {

        int count = getBulletCount(spellLevel);
        float damage = getDamage(spellLevel, entity);

        // 1. 方向ベクトルの計算
        Vec3 look = entity.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.01) right = look.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = right.cross(look).normalize();

        // 2. 発射基準点（後ろ BACK_DISTANCE + 上 1.0）
        Vec3 baseOrigin = entity.getEyePosition()
                .subtract(look.scale(BACK_DISTANCE))
                .add(0, 5.0, 0);

        // 3. 長方形グリッドの計算
        int columns = (int) Math.ceil(Math.sqrt(count * (RECT_WIDTH / RECT_HEIGHT)));
        int rows = (int) Math.ceil((double) count / columns);
        float xStep = (columns > 1) ? RECT_WIDTH / (columns - 1) : 0;
        float yStep = (rows > 1) ? RECT_HEIGHT / (rows - 1) : 0;

        for (int i = 0; i < count; i++) {
            int row = i / columns;
            int col = i % columns;

            float xOff = (col * xStep) - (RECT_WIDTH / 2f);
            float yOff = (row * yStep) - (RECT_HEIGHT / 2f);

            // 各弾の初期位置：基準点から横(right)と縦(up)にずらす
            Vec3 bulletOrigin = baseOrigin.add(right.scale(xOff)).add(up.scale(yOff));

            // 4. 弾道の決定
            // 収束させず、プレイヤーの向いている方向 (look) へ完全に平行に飛ばす
            Vec3 finalDir = look;

            spawnMissile(world, entity, bulletOrigin, finalDir, damage);
        }

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
