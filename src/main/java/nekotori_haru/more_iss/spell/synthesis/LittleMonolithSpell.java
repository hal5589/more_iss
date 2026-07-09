package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nekotori_haru.more_iss.entity.LittleMonolithEntity;

import java.util.List;

@AutoSpellConfig
public class LittleMonolithSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            new ResourceLocation("more_iss", "little_monolith");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public LittleMonolithSpell() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 40;
        this.manaCostPerLevel = 10;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 0;
    }

    // ===== UI表示 =====
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius",
                        Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.health",
                        Utils.stringTruncation(getHealth(spellLevel, caster), 1))
        );
    }

    // ===== 各種計算メソッド =====
    private float getRadius(int spellLevel, LivingEntity caster) {
        return 6 + spellLevel * 0.75f;
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 0.3f;
    }

    // 体力：基礎100 + レベル×20
    private float getHealth(int spellLevel, LivingEntity caster) {
        return 100 + spellLevel * 20;
    }

    // ===== スペル発動 =====
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData data) {
        if (level.isClientSide) return;

        // 視線先を計算
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = start.add(look.scale(32));

        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        Vec3 spawnPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = new BlockPos((int) hit.getLocation().x, (int) hit.getLocation().y, (int) hit.getLocation().z);
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            spawnPos = new Vec3(hit.getLocation().x, y, hit.getLocation().z);
        } else {
            spawnPos = caster.position();
        }

        // モノリスを生成（制限時間なし）
        float health = getHealth(spellLevel, caster);
        LittleMonolithEntity monolith = LittleMonolithEntity.create(level, caster, spellLevel, health);
        monolith.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        // ★ setLifeTicks の呼び出しを削除（永続化）
        level.addFreshEntity(monolith);

        // 召喚パーティクル
        if (level instanceof ServerLevel server) {
            MagicManager.spawnParticles(server, ParticleHelper.ENDER_SPARKS,
                    spawnPos.x, spawnPos.y + 1, spawnPos.z,
                    20, 0.5, 0.5, 0.5, 0.05, false);
        }

        super.onCast(level, spellLevel, caster, castSource, data);
    }
}