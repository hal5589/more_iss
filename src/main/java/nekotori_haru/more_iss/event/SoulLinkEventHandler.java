package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "more_iss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoulLinkEventHandler {

    private static final String NBT_KEY = "SoulLinkResistances";

    /**
     * NBT（永続データ領域）に耐性デバフのリストを書き込む
     */
    public static void saveResistances(LivingEntity entity, List<MobEffect> effects) {
        CompoundTag persistentData = entity.getPersistentData();
        ListTag list = new ListTag();
        for (MobEffect effect : effects) {
            ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            if (rl != null) {
                list.add(StringTag.valueOf(rl.toString()));
            }
        }
        persistentData.put(NBT_KEY, list);
    }

    /**
     * NBTから耐性デバフのリストを読み出す
     */
    private static List<MobEffect> loadResistances(LivingEntity entity) {
        List<MobEffect> effects = new ArrayList<>();
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(NBT_KEY, Tag.TAG_LIST)) {
            ListTag list = persistentData.getList(NBT_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation rl = new ResourceLocation(list.getString(i));
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    // ─── 対策①：新しくデバフがかかるのを完全に拒否する ───
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null) return;

        // 「魂の保護」バフを持っている場合のみ判定
        if (entity.hasEffect(ModEffects.SOUL_LINK_PROTECTION.get())) {
            List<MobEffect> blockedEffects = loadResistances(entity);
            if (blockedEffects.contains(incoming.getEffect())) {
                event.setResult(Event.Result.DENY); // 完全に拒否
            }
        }
    }

    // ─── 対策②：すり抜けて付与されたデバフを1/20秒で強制消去する（超強力） ───
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.hasEffect(ModEffects.SOUL_LINK_PROTECTION.get())) {
            List<MobEffect> blockedEffects = loadResistances(entity);
            if (!blockedEffects.isEmpty()) {
                for (MobEffect effect : blockedEffects) {
                    if (entity.hasEffect(effect)) {
                        entity.removeEffect(effect); // 見つけ次第即座に消す
                    }
                }
            }
        } else {
            // バフが切れていたらNBTデータも綺麗にお掃除
            if (entity.getPersistentData().contains(NBT_KEY)) {
                entity.getPersistentData().remove(NBT_KEY);
            }
        }
    }
}