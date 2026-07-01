package nekotori_haru.more_iss.entity;

import nekotori_haru.more_iss.api.BeamType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class BaseBeamVisualEntity extends Entity {
    public static final float lifetime = 20.0f;

    public double distance = 0.0;
    private BeamType beamType = BeamType.FLAME;
    private Vec3 direction = Vec3.ZERO;

    @Nullable
    private Entity owner;

    private static final EntityDataAccessor<Float> DATA_DISTANCE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_BEAM_TYPE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Y =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);

    public BaseBeamVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // 従来のコンストラクタ（互換性維持：真下固定）
    @Deprecated
    public BaseBeamVisualEntity(EntityType<?> type, Level level, Entity owner, double distance, BeamType beamType) {
        this(type, level);
        this.owner = owner;
        this.distance = distance;
        this.beamType = beamType;
        this.direction = new Vec3(0, -1, 0);

        this.entityData.set(DATA_DISTANCE, (float) distance);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());
        this.entityData.set(DATA_DIR_X, 0.0f);
        this.entityData.set(DATA_DIR_Y, -1.0f);
        this.entityData.set(DATA_DIR_Z, 0.0f);

        setRotationFromDirection(this.direction);
    }

    // 新しいコンストラクタ（任意の方向・開始位置を指定）
    public BaseBeamVisualEntity(EntityType<?> type, Level level, Entity owner,
                                Vec3 startPos, Vec3 direction, double distance, BeamType beamType) {
        this(type, level);
        this.owner = owner;
        this.distance = distance;
        this.beamType = beamType;
        this.direction = direction.normalize();

        this.setPos(startPos.x, startPos.y, startPos.z);

        this.entityData.set(DATA_DISTANCE, (float) distance);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());
        this.entityData.set(DATA_DIR_X, (float) this.direction.x);
        this.entityData.set(DATA_DIR_Y, (float) this.direction.y);
        this.entityData.set(DATA_DIR_Z, (float) this.direction.z);

        setRotationFromDirection(this.direction);
    }

    // ★ Minecraft 標準の回転ルールに修正（Iron's レンダラーと互換）
    private void setRotationFromDirection(Vec3 dir) {
        if (dir.lengthSqr() < 1.0E-8) return;

        // Yaw: 水平回転（Minecraft標準: dir.x = -sin(yaw)*cos(pitch), dir.z = cos(yaw)*cos(pitch)）
        // ★修正: atan2(-dir.x, -dir.z) だとz符号が余分に反転しyawが180°ズレていたため atan2(-dir.x, dir.z) に修正
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI);
        // Pitch: 上下回転（真上 -90、真下 90）
        float pitch = (float) (Math.asin(-dir.y) * 180.0 / Math.PI);

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DISTANCE, 0.0f);
        this.entityData.define(DATA_BEAM_TYPE, BeamType.FLAME.name());
        this.entityData.define(DATA_DIR_X, 0.0f);
        this.entityData.define(DATA_DIR_Y, -1.0f);
        this.entityData.define(DATA_DIR_Z, 0.0f);
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

            float dx = this.entityData.get(DATA_DIR_X);
            float dy = this.entityData.get(DATA_DIR_Y);
            float dz = this.entityData.get(DATA_DIR_Z);
            Vec3 syncedDir = new Vec3(dx, dy, dz);
            if (syncedDir.lengthSqr() > 1.0E-8) {
                this.direction = syncedDir.normalize();
                setRotationFromDirection(this.direction);
            }
        }

        if (this.tickCount >= lifetime) {
            this.discard();
        }
    }

    public BeamType getBeamType() { return this.beamType; }
    public double getDistance() { return this.distance; }
    public Vec3 getBeamDirection() { return this.direction; }
    @Nullable public Entity getOwner() { return this.owner; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}