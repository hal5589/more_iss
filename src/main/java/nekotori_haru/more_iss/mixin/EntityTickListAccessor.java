/*
 * This file contains code originally from the "NoSugar" project.
 * MIT License - Copyright (c) NoSugar Authors.
 */
package nekotori_haru.more_iss.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import java.lang.reflect.Field;

public class EntityTickListAccessor {

    // 通常名（開発環境）とSRG名（製品版）の両方でフィールドを安全に引っこ抜くコアメソッド
    @SuppressWarnings("unchecked")
    private static Int2ObjectMap<Entity> getField(EntityTickList instance, String mcpName, String srgName) {
        try {
            Field f;
            try {
                f = EntityTickList.class.getDeclaredField(mcpName);
            } catch (NoSuchFieldException e) {
                f = EntityTickList.class.getDeclaredField(srgName);
            }
            f.setAccessible(true);
            return (Int2ObjectMap<Entity>) f.get(instance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void setField(EntityTickList instance, String mcpName, String srgName, Int2ObjectMap<Entity> value) {
        try {
            Field f;
            try {
                f = EntityTickList.class.getDeclaredField(mcpName);
            } catch (NoSuchFieldException e) {
                f = EntityTickList.class.getDeclaredField(srgName);
            }
            f.setAccessible(true);
            f.set(instance, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 外部から安全に呼び出すための静的エントリーポイント
    public static Int2ObjectMap<Entity> getActive(EntityTickList instance) { return getField(instance, "active", "f_119685_"); }
    public static void setActive(EntityTickList instance, Int2ObjectMap<Entity> active) { setField(instance, "active", "f_119685_", active); }
    public static Int2ObjectMap<Entity> getPassive(EntityTickList instance) { return getField(instance, "passive", "f_119686_"); }
    public static void setPassive(EntityTickList instance, Int2ObjectMap<Entity> passive) { setField(instance, "passive", "f_119686_", passive); }
    public static Int2ObjectMap<Entity> getIterated(EntityTickList instance) { return getField(instance, "iterated", "f_119687_"); }
}