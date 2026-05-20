/*
 * This file contains code originally from the "NoSugar" project.
 * MIT License - Copyright (c) NoSugar Authors.
 */
package nekotori_haru.more_iss.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import java.lang.reflect.Field;

public class ServerLevelAccessor {

    @SuppressWarnings("unchecked")
    public static PersistentEntitySectionManager<Entity> getEntityManager(ServerLevel level) {
        try {
            Field f;
            try { f = ServerLevel.class.getDeclaredField("entityManager"); }
            catch (NoSuchFieldException e) { f = ServerLevel.class.getDeclaredField("f_8555_"); }
            f.setAccessible(true);
            return (PersistentEntitySectionManager<Entity>) f.get(level);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public static EntityTickList getEntityTickList(ServerLevel level) {
        try {
            Field f;
            try { f = ServerLevel.class.getDeclaredField("entityTickList"); }
            catch (NoSuchFieldException e) { f = ServerLevel.class.getDeclaredField("f_8557_"); }
            f.setAccessible(true);
            return (EntityTickList) f.get(level);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}