/*
 * This file contains code originally from the "NoSugar" project.
 * MIT License - Copyright (c) NoSugar Authors.
 * Modified to use raw reflection to minimize standalone mixin accessors.
 */
package nekotori_haru.more_iss.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public class SynchedEntityDataUtil {
    @SuppressWarnings("unchecked")
    public static <T> void forceSet(SynchedEntityData data, EntityDataAccessor<T> accessor, T value) {
        try {
            // SynchedEntityData の itemsById (登録データマップ) を取得
            Field itemsField = SynchedEntityData.class.getDeclaredField("itemsById");
            itemsField.setAccessible(true);
            Map<Integer, SynchedEntityData.DataItem<?>> itemsById = (Map<Integer, SynchedEntityData.DataItem<?>>) itemsField.get(data);

            SynchedEntityData.DataItem<T> item = (SynchedEntityData.DataItem<T>) itemsById.get(accessor.getId());

            if (item != null && !Objects.equals(item.getValue(), value)) {
                // DataItem の value フィールドを直接書き換え
                Field valueField = SynchedEntityData.DataItem.class.getDeclaredField("value");
                valueField.setAccessible(true);
                valueField.set(item, value);

                // エンティティのデータ更新通知を強制発火
                Method getEntityMethod = SynchedEntityData.class.getDeclaredMethod("getEntity");
                getEntityMethod.setAccessible(true);
                net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) getEntityMethod.invoke(data);
                entity.onSyncedDataUpdated(accessor);

                // ネットワーク送信フラグ (isDirty) を強制ON
                Field dirtyField = SynchedEntityData.DataItem.class.getDeclaredField("dirty");
                dirtyField.setAccessible(true);
                dirtyField.set(item, true);

                Field isDirtyField = SynchedEntityData.class.getDeclaredField("isDirty");
                isDirtyField.setAccessible(true);
                isDirtyField.set(data, true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}