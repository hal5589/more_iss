package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThunderboltFlashProjectile extends LightningLanceProjectile {
    private int age = 0;
    private final int WAIT_TIME = 10; // 0.5秒
    private LivingEntity target;

    // 💡 1. ネットワーク同期やスポーン時にシステムが呼ぶコンストラクタ
    public ThunderboltFlashProjectile(EntityType<? extends LightningLanceProjectile> type, Level level) {
        super(type, level);
    }

    // 💡 2. 魔法クラス（ThunderboltFlash）から新しく生成する時に呼ぶコンストラクタ
    // 第1引数に引数名「entityType」として明示的に受け取ることで、privateアクセスエラーを完全に回避します
    public ThunderboltFlashProjectile(EntityType<? extends LightningLanceProjectile> entityType, Level level, LivingEntity shooter, LivingEntity target) {
        super(entityType, level);
        this.setOwner(shooter);
        this.target = target;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (age < WAIT_TIME) {
                this.setDeltaMovement(Vec3.ZERO); // 停止
            } else if (age == WAIT_TIME) {
                // 発射
                Vec3 dir = target != null && target.isAlive()
                        ? target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(this.position()).normalize()
                        : this.getLookAngle();
                this.shoot(dir.scale(2.0));
            } else if (target != null && target.isAlive()) {
                // ホーミング
                Vec3 motion = this.getDeltaMovement();
                Vec3 targetDir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(this.position()).normalize();
                this.setDeltaMovement(motion.scale(0.9).add(targetDir.scale(0.2)).normalize().scale(motion.length()));
            }
        }
        super.tick();
        age++;
    }
}