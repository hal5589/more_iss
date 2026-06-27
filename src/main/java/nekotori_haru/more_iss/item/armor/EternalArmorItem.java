package nekotori_haru.more_iss.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import nekotori_haru.more_iss.client.renderer.EternalArmorRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

public class EternalArmorItem extends ArmorItem implements GeoItem {

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        return stack;
    }

        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        // ⭐ 防具強度（各スロットごとに定義）
        private static final float TOUGHNESS_HELMET = 7.0F;
        private static final float TOUGHNESS_CHESTPLATE = 17.0F;
        private static final float TOUGHNESS_LEGGINGS = 17.0F;
        private static final float TOUGHNESS_BOOTS = 7.0F;

        // UUID（16進数で統一）
        private static final UUID TOUGHNESS_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        private static final UUID COOLDOWN_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");
        private static final UUID CAST_TIME_MODIFIER_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-345678901234");
        private static final UUID SPELL_POWER_MODIFIER_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-def0-456789012345");
        private static final UUID MAX_MANA_MODIFIER_UUID = UUID.fromString("e5f6a7b8-c9d0-1234-ef01-567890123456");
        private static final UUID MANA_REGEN_MODIFIER_UUID = UUID.fromString("f6a7b8c9-d0e1-2345-f012-678901234567");
        private static final UUID MAGIC_RESIST_MODIFIER_UUID = UUID.fromString("a7b8c9d0-e1f2-3456-0123-789012345678");

        public EternalArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
            super(material, type, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            if (slot != this.getEquipmentSlot()) {
                return super.getDefaultAttributeModifiers(slot);
            }

            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(slot));

            // 防具強度をスロット別に設定（if-else 文）
            float toughness;
            switch (this.getType()) {
                case HELMET:
                    toughness = TOUGHNESS_HELMET;
                    break;
                case CHESTPLATE:
                    toughness = TOUGHNESS_CHESTPLATE;
                    break;
                case LEGGINGS:
                    toughness = TOUGHNESS_LEGGINGS;
                    break;
                case BOOTS:
                    toughness = TOUGHNESS_BOOTS;
                    break;
                default:
                    toughness = 0.0F;
                    break;
            }
            builder.put(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_MODIFIER_UUID, "Armor toughness", toughness, AttributeModifier.Operation.ADDITION));

            // ⭐ Iron's Spells 属性をスロット別に追加（すべて MULTIPLY_BASE でパーセント表示に対応）
            switch (this.getType()) {
                case HELMET:
                    // クールタイム短縮 +77%
                    builder.put(AttributeRegistry.COOLDOWN_REDUCTION.get(),
                            new AttributeModifier(COOLDOWN_MODIFIER_UUID, "Cooldown reduction", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    builder.put(AttributeRegistry.MAX_MANA.get(),
                            new AttributeModifier(MAX_MANA_MODIFIER_UUID, "Max mana", 777, AttributeModifier.Operation.ADDITION));

                    // 詠唱時間短縮 +77%
                    builder.put(AttributeRegistry.CAST_TIME_REDUCTION.get(),
                            new AttributeModifier(CAST_TIME_MODIFIER_UUID, "Cast time reduction", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    break;
                case CHESTPLATE:
                    // 魔法威力 +77%
                    builder.put(AttributeRegistry.SPELL_POWER.get(),
                            new AttributeModifier(SPELL_POWER_MODIFIER_UUID, "Spell power", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    break;
                case LEGGINGS:
                    // 最大マナ +77%
                    builder.put(AttributeRegistry.MAX_MANA.get(),
                            new AttributeModifier(MAX_MANA_MODIFIER_UUID, "Max mana", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    // マナ再生 +77%
                    builder.put(AttributeRegistry.MANA_REGEN.get(),
                            new AttributeModifier(MANA_REGEN_MODIFIER_UUID, "Mana regen", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    break;
                case BOOTS:
                    // 魔法耐性 +77%
                    builder.put(AttributeRegistry.SPELL_RESIST.get(),
                            new AttributeModifier(MAGIC_RESIST_MODIFIER_UUID, "Magic resist", 0.77, AttributeModifier.Operation.MULTIPLY_BASE));
                    break;
            }

            return builder.build();
        }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;  // false = ダメージを受けない = 不可壊
    }


    @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private EternalArmorRenderer renderer;

                @Override
                public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                        net.minecraft.world.entity.LivingEntity livingEntity,
                        ItemStack itemStack,
                        EquipmentSlot equipmentSlot,
                        net.minecraft.client.model.HumanoidModel<?> original) {
                    if (this.renderer == null) {
                        this.renderer = new EternalArmorRenderer();
                    }
                    this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                    return this.renderer;
                }
            });
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // 防具はアニメーション制御を必要としないため空実装
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return this.cache;
        }
    }