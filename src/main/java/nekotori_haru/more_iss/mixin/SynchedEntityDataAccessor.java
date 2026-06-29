package nekotori_haru.more_iss.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * SynchedEntityData#entity (private final) を読み取るためのAccessor。
 *
 * fantasy_ending が SynchedEntityData.set(...) を直接呼んで体力デルタを
 * 書き込む際、「どの Entity に対する書き込みか」を判定する必要があるため、
 * 本来 private な entity フィールドを公開する。
 */
@Mixin(SynchedEntityData.class)
public interface SynchedEntityDataAccessor {

    @Accessor("entity")
    Entity more_iss$getEntity();
}