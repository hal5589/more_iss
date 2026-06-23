package nekotori_haru.more_iss.entity;

import nekotori_haru.more_iss.api.BeamType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class BaseBeamVisualEntity extends Entity {
    public static final float lifetime = 20.0f;

    public double distance = 0.0;
    private BeamType beamType = BeamType.FLAME;

    @Nullable
    private Entity owner;

    private static final EntityDataAccessor<Float> DATA_DISTANCE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_BEAM_TYPE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.STRING);

    public BaseBeamVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public BaseBeamVisualEntity(EntityType<?> type, Level level, Entity owner, double distance, BeamType beamType) {
        this(type, level);
        this.owner = owner;
        this.distance = distance;
        this.beamType = beamType;

        this.entityData.set(DATA_DISTANCE, (float) distance);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());

        // ⭐ ビームは常に真下を向く（XRot=90）
        this.setXRot(90.0f);
        this.setYRot(0.0f);
        this.yRotO = 0.0f;
        this.xRotO = 90.0f;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DISTANCE, 0.0f);
        this.entityData.define(DATA_BEAM_TYPE, BeamType.FLAME.name());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.distance = this.entityData.get(DATA_DISTANCE);
            try {
                this.beamType = BeamType.valueOf(this.entityData.get(DATA_BEAM_TYPE));
            } catch (IllegalArgumentException e) {
                this.beamType = BeamType.FLAME;
            }
        }

        if (this.tickCount >= lifetime) {
            this.discard();
        }
    }

    public BeamType getBeamType() {
        return this.beamType;
    }

    public double getDistance() {
        return this.distance;
    }

    @Nullable
    public Entity getOwner() {
        return this.owner;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}