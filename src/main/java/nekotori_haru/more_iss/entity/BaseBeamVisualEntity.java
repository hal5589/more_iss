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
    // 🌟 1. レンダラーや呪文がアクセスする変数・定数をしっかり定義
    public static final float lifetime = 8.0f; // ビームの生存時間（10 Tick）
    public double distance = 0.0;
    private BeamType beamType = BeamType.FLAME; // デフォルトで火属性

    @Nullable
    private Entity owner;

    // 🌟 2. 1.20.1のネットワーク同期用のデータアクセサーを定義
    // クライアント側（レンダラー）にビームの長さと属性を正しく伝えるために必要です
    private static final EntityDataAccessor<Float> DATA_DISTANCE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_BEAM_TYPE =
            SynchedEntityData.defineId(BaseBeamVisualEntity.class, EntityDataSerializers.STRING);

    // バニラやクライアント側が生成に使うコンストラクタ
    public BaseBeamVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // 壁を貫通する（見た目だけなので）
    }

    // 🌟 3. 呪文クラス（FlameRaySpell）から呼び出される本命のコンストラクタ
    public BaseBeamVisualEntity(EntityType<?> type, Level level, Entity owner, double distance, BeamType beamType) {
        this(type, level);
        this.owner = owner;
        this.distance = distance;
        this.beamType = beamType;

        // クライアント同期用データに即時セット
        this.entityData.set(DATA_DISTANCE, (float) distance);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());

        // 向きをプレイヤーに合わせる
        this.setXRot(owner.getXRot());
        this.setYRot(owner.getYRot());
        this.yRotO = owner.getYRot();
        this.xRotO = owner.getXRot();
    }

    // 🌟 4. Entityクラスの必須抽象メソッド：データアクセサーの初期化
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DISTANCE, 0.0f);
        this.entityData.define(DATA_BEAM_TYPE, BeamType.FLAME.name());
    }

    // 🌟 5. 毎Tick実行される処理（一定時間経ったら消えるようにする）
    @Override
    public void tick() {
        super.tick();

        // クライアント側（レンダラー側）でも同期されたデータを常に読み込む
        if (this.level().isClientSide) {
            this.distance = this.entityData.get(DATA_DISTANCE);
            try {
                this.beamType = BeamType.valueOf(this.entityData.get(DATA_BEAM_TYPE));
            } catch (IllegalArgumentException e) {
                this.beamType = BeamType.FLAME;
            }
        }

        // 寿命が来たら消滅
        if (this.tickCount >= lifetime) {
            this.discard();
        }
    }

    // レンダラーから属性（カラーコード等）を取得するために使うメソッド
    public BeamType getBeamType() {
        return this.beamType;
    }

    // レンダラーのカリングチェックで使うメソッド
    @Nullable
    public Entity getOwner() {
        return this.owner;
    }

    // 🌟 6. Entityクラスの必須抽象メソッド：セーブ・ロード処理（見た目用なので中身は空でOK）
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}