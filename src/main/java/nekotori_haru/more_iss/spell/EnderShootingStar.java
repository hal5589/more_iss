package nekotori_haru.more_iss.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
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
public class EnderShootingStar extends AbstractSpell {

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath("more_iss", "ender_shooting_star");

    // --- 弾幕のパラメータ ---
    private static final float RECT_WIDTH = 10.0f;     // 横の広がり
    private static final float RECT_HEIGHT = 5.0f;    // 縦の広がり
    private static final float BACK_DISTANCE = 4.0f;  // 背後への距離
    private static final float UP_OFFSET = 3f;      // 頭上への高さ
    private static final int[] BULLET_COUNT = { 10, 15, 20, 25, 30 }; // レベルごとの弾数

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public EnderShootingStar() {
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 4;
        this.baseManaCost = 250;
        this.manaCostPerLevel = 30;
        this.castTime = 20;
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
        return CastType.LONG;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal("Projectiles: " + getBulletCount(spellLevel)),
                Component.literal(String.format("Area: %.1fx%.1f", RECT_WIDTH, RECT_HEIGHT))
        );
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity,
                       CastSource castSource, MagicData playerMagicData) {

        int count = getBulletCount(spellLevel);
        float damage = getDamage(spellLevel, entity);

        // 1. 視線方向
        Vec3 look = entity.getLookAngle().normalize();

        // 2. 目標地点
        Vec3 targetPos = entity.getEyePosition().add(look.scale(64.0));

        // 3. 召喚の基準点（プレイヤーの少し上）
        Vec3 baseOrigin = entity.getEyePosition().add(0, UP_OFFSET, 0);

        // 4. 射撃軸の計算（中心点からターゲットへ）
        Vec3 centerSpawnPos = baseOrigin.subtract(look.scale(BACK_DISTANCE));
        Vec3 centerLook = targetPos.subtract(centerSpawnPos).normalize();

        // 射撃軸に正対する「右」と「上」
        Vec3 right = centerLook.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.01) right = centerLook.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = right.cross(centerLook).normalize();

        // 5. ランダム配置で発射
        for (int i = 0; i < count; i++) {
            // -0.5 ~ 0.5 で中央揃え
            float xOff = (world.random.nextFloat() - 0.5f) * RECT_WIDTH;
            float yOff = (world.random.nextFloat() - 0.5f) * RECT_HEIGHT;
            float zOff = (world.random.nextFloat() - 0.5f) * 8.0f; // 奥行きのバラつき

            Vec3 bulletOrigin = baseOrigin
                    .add(centerLook.scale(-BACK_DISTANCE + zOff))
                    .add(right.scale(xOff))
                    .add(up.scale(yOff));

            spawnMissile(world, entity, bulletOrigin, centerLook, damage);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnMissile(Level world, LivingEntity caster,
                              Vec3 origin, Vec3 direction, float damage) {
        MagicMissileProjectile missile = new MagicMissileProjectile(world, caster);
        missile.setPos(origin.x, origin.y, origin.z);
        missile.shoot(direction);
        missile.setDamage(damage);
        world.addFreshEntity(missile);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDER_EYE_LAUNCH);
    }

    private int getBulletCount(int level) {
        return BULLET_COUNT[Math.min(level - 1, BULLET_COUNT.length - 1)];
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) * .5f;
    }
}