package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.PolychromaticBeamEntity;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class PolychromaticBeamSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "polychromatic_beam");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .setAllowCrafting(true)
            .build();

    public PolychromaticBeamSpell() {
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 2;
        this.manaCostPerLevel = 10;
        this.baseManaCost = 30;
        this.castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new BeamCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof BeamCastData) {
            return;
        }

        createBeam(level, spellLevel, entity, playerMagicData);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof BeamCastData beamData && level instanceof ServerLevel server) {
            PolychromaticBeamEntity beam = beamData.getEntity(server);
            if (beam != null && beam.isAlive()) {
                Vec3 beamPos = calculateBeamPosition(entity);
                beam.moveTo(beamPos.x, beamPos.y, beamPos.z, entity.getYRot(), entity.getXRot());
                beam.updateLength(32.0f, level);
            } else {
                createBeam(level, spellLevel, entity, playerMagicData);
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData.getAdditionalCastData() instanceof BeamCastData castData && level instanceof ServerLevel serverLevel) {
            PolychromaticBeamEntity beam = castData.getEntity(serverLevel);
            if (beam != null && beam.isAlive()) {
                beam.setSpellActive(false);
                beam.discard();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void createBeam(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) return;

        float damage = getDamage(spellLevel, entity);
        Random random = new Random();
        int effectType = random.nextInt(3);
        float maxRange = 32.0f;

        PolychromaticBeamEntity beam = new PolychromaticBeamEntity(level, entity, damage, effectType, maxRange);
        beam.setSpellActive(true);

        Vec3 beamPos = calculateBeamPosition(entity);
        beam.moveTo(beamPos.x, beamPos.y, beamPos.z, entity.getYRot(), entity.getXRot());
        beam.setup(0x00000000, 0x00000000, maxRange, 0.25f);
        beam.setDamage(damage);
        beam.updateLength(maxRange, level);

        level.addFreshEntity(beam);

        BeamCastData castData = new BeamCastData();
        castData.setEntity(beam);
        playerMagicData.setAdditionalCastData(castData);
    }

    private Vec3 calculateBeamPosition(LivingEntity entity) {
        return entity.getEyePosition(1.0f).add(0, -0.4, 0).add(entity.getLookAngle().scale(0.75f));
    }

    // ⭐ 整数ベースのダメージ計算
    // レベル1: 1 + 1 + (スペルパワー/2) = 2 + (スペルパワー/2)
    private float getDamage(int spellLevel, LivingEntity caster) {
        float spellPower = getSpellPower(spellLevel, caster);
        return this.baseSpellPower + (spellLevel * this.spellPowerPerLevel) + (float)Math.floor(spellPower / 2);
    }

    public static class BeamCastData implements ICastDataSerializable {
        private UUID entityId;

        public void setEntity(PolychromaticBeamEntity entity) {
            this.entityId = entity.getUUID();
        }

        public PolychromaticBeamEntity getEntity(ServerLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof PolychromaticBeamEntity) {
                return (PolychromaticBeamEntity) entity;
            }
            return null;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buf) {
            buf.writeUUID(entityId);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buf) {
            entityId = buf.readUUID();
        }

        @Override
        public void reset() {
            entityId = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("EntityId", entityId);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            entityId = nbt.getUUID("EntityId");
        }
    }
}