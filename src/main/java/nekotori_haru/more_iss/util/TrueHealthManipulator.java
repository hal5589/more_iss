package nekotori_haru.more_iss.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ── 🌟 概念破砕・絶対領域貫通 HP操作エンジン ──
 */
public class TrueHealthManipulator {

    private static final Map<String, CachedHealthData> CACHE = new ConcurrentHashMap<>();

    private static VarHandle DATA_ITEM_VALUE_HANDLE = null;
    private static Field ITEMS_BY_ID_FIELD = null;

    static {
        try {
            for (Field field : SynchedEntityData.DataItem.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && field.getType() == Object.class) {
                    field.setAccessible(true);
                    DATA_ITEM_VALUE_HANDLE = MethodHandles.lookup().unreflectVarHandle(field);
                    break;
                }
            }

            for (Field field : SynchedEntityData.class.getDeclaredFields()) {
                if (field.getType() == Map.class) {
                    field.setAccessible(true);
                    ITEMS_BY_ID_FIELD = field;
                    break;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void scanAndCacheHealthField(LivingEntity entity) {
        Class<? extends LivingEntity> clazz = entity.getClass();
        String className = clazz.getName();

        if (CACHE.containsKey(className)) return;

        if (entity instanceof Player || className.startsWith("net.minecraft.")) {
            CACHE.put(className, new CachedHealthData(6, true));
            return;
        }

        try {
            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != LivingEntity.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == EntityDataAccessor.class) {
                        String name = field.getName().toUpperCase().replace("_", "");
                        if (name.contains("HEALTH") && !name.contains("MAX")) {
                            field.setAccessible(true);

                            // 💡 インターフェースを使わず直接キャストしてIDを取得
                            EntityDataAccessor<?> accessor = (EntityDataAccessor<?>) field.get(null);
                            int id = accessor.getId();

                            SynchedEntityData entityData = entity.getEntityData();
                            if (ITEMS_BY_ID_FIELD != null) {
                                Map<Integer, SynchedEntityData.DataItem<?>> items =
                                        (Map<Integer, SynchedEntityData.DataItem<?>>) ITEMS_BY_ID_FIELD.get(entityData);
                                SynchedEntityData.DataItem<?> item = items.get(id);

                                if (item != null) {
                                    boolean isFloat = item.getValue() instanceof Float;
                                    CACHE.put(className, new CachedHealthData(id, isFloat));
                                    return;
                                }
                            }
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        CACHE.put(className, new CachedHealthData(6, true));
    }

    public static void forceSetTrueHealth(LivingEntity entity, float targetHealth) {
        if (entity.level().isClientSide()) return;

        scanAndCacheHealthField(entity);

        String className = entity.getClass().getName();
        CachedHealthData config = CACHE.get(className);

        try {
            entity.setHealth(targetHealth);
        } catch (Throwable ignored) {}

        if (config != null && ITEMS_BY_ID_FIELD != null && DATA_ITEM_VALUE_HANDLE != null) {
            try {
                SynchedEntityData entityData = entity.getEntityData();
                Map<Integer, SynchedEntityData.DataItem<?>> items =
                        (Map<Integer, SynchedEntityData.DataItem<?>>) ITEMS_BY_ID_FIELD.get(entityData);

                SynchedEntityData.DataItem<?> dataItem = items.get(config.index);
                if (dataItem != null) {
                    Object newValue = config.isFloat ? targetHealth : (double) targetHealth;

                    if (ObjectUtils.notEqual(newValue, dataItem.getValue())) {
                        DATA_ITEM_VALUE_HANDLE.set(dataItem, newValue);

                        // 💡 dataItem の setDirty だけでネットワーク同期フラグが正常に立ちます！
                        dataItem.setDirty(true);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static class CachedHealthData {
        final int index;
        final boolean isFloat;
        CachedHealthData(int index, boolean isFloat) {
            this.index = index;
            this.isFloat = isFloat;
        }
    }
}