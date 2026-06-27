package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

import nekotori_haru.more_iss.network.ModNetwork;
import nekotori_haru.more_iss.network.PacketBossBarSync;
import nekotori_haru.more_iss.registry.ModItems;
import nekotori_haru.more_iss.registry.ModSpells;

public class EternalWizardEntity extends AbstractSpellCastingMob implements Enemy {

    private static final Random RANDOM = new Random();
    private static final double FLOAT_HEIGHT = 5.0;
    private static final double MAX_HEALTH_VALUE = 7777.0;
    private static final double MOVEMENT_SPEED = 0.45;

    private static final int DEFAULT_MULTI_CAST_GAP = 6;
    private static final double BOSS_BAR_SYNC_RADIUS = 96.0;

    // ========== 行動パターン管理 ==========
    private ActionPattern currentPattern;
    private int currentActionIndex = 0;
    private int actionTimer = 0;
    private boolean patternRunning = false;
    private int phaseInterval = 20;
    private int patternCooldown = 0;
    private boolean waitingForCastComplete = false;
    private boolean pendingAdvance = false;

    private int multiCastStep = 0;
    private int currentSpellCount = 1;
    private boolean isMoving = false;
    private int patternCheckCounter = 0;

    // 表示用
    private String currentCastingSpellName = "";
    private int displayTimer = 0;

    // 周回移動用
    private double orbitAngle = 0;
    private boolean isOrbiting = false;

    // 除外魔法リスト
    private static final List<String> EXCLUDED_SPELLS = List.of(
            "HealSpell", "GreaterHealSpell", "HealingCircleSpell", "BlessingOfLifeSpell",
            "FortifySpell", "OakskinSpell", "AscensionSpell", "ChargeSpell",
            "EvasionSpell", "InvisibilitySpell", "AbyssalShroudSpell", "AngelWingSpell",
            "GluttonySpell", "HasteSpell", "EldritchBlastSpell",
            "CleanseSpell", "PlanarSightSpell", "HeartstopSpell",
            "DisintegrationSpell", "OverburstBloodSpell"
    );

    private List<ActionPattern> allPatterns = new ArrayList<>();
    private Map<String, ActionPattern> patternMap = new HashMap<>();
    private String currentPatternName = "";

    // ============================================================
    //  コンストラクタ
    // ============================================================

    public EternalWizardEntity(EntityType<? extends EternalWizardEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 77777;
        this.moveControl = new FlyingMoveControl(this);
        this.setNoGravity(true);
        initPatterns();
        updatePhaseInterval();
        selectRandomPattern();

        if (!level.isClientSide) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ETERNAL_HALO.get()));
            this.setDropChance(EquipmentSlot.HEAD, 1.0F);

            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ETERNAL_ROBE.get()));
            this.setDropChance(EquipmentSlot.CHEST, 1.0F);

            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.ETERNAL_LEGGINGS.get()));
            this.setDropChance(EquipmentSlot.LEGS, 1.0F);

            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.ETERNAL_BOOTS.get()));
            this.setDropChance(EquipmentSlot.FEET, 1.0F);
        }
    }

    // ============================================================
    //  ★★★ ヘルパーメソッド ★★★
    // ============================================================

    private List<AbstractSpell> listOf(AbstractSpell... spells) {
        List<AbstractSpell> list = new ArrayList<>();
        for (AbstractSpell s : spells) {
            if (s != null && !EXCLUDED_SPELLS.contains(s.getClass().getSimpleName())) {
                list.add(s);
            }
        }
        return list;
    }

    private PatternAction waitAction(int ticks) {
        return new PatternAction(PatternActionType.WAIT, ticks);
    }

    private PatternAction moveAction(int ticks) {
        return new PatternAction(PatternActionType.MOVE, ticks);
    }

    private PatternAction castAction(AbstractSpell spell) {
        return new PatternAction(PatternActionType.CAST_SPELL, spell, 0);
    }

    private PatternAction castAction(AbstractSpell spell, int level) {
        return new PatternAction(PatternActionType.CAST_SPELL, spell, level);
    }

    private PatternAction castMultiAction(AbstractSpell... spells) {
        return new PatternAction(PatternActionType.CAST_MULTI_INSTANT, listOf(spells));
    }

    private PatternAction castMultiIntervalAction(int interval, AbstractSpell... spells) {
        return new PatternAction(PatternActionType.CAST_MULTI_INTERVAL, listOf(spells), interval);
    }

    private PatternAction castMultiCustomAction(CastEntry... entries) {
        return new PatternAction(PatternActionType.CAST_MULTI_CUSTOM, entries);
    }

    private PatternAction castRepeatAction(int count, AbstractSpell spell) {
        return new PatternAction(PatternActionType.CAST_REPEAT, count, spell, 0);
    }

    private PatternAction castRepeatAction(int count, AbstractSpell spell, int level) {
        return new PatternAction(PatternActionType.CAST_REPEAT, count, spell, level);
    }

    private PatternAction castAndWaitAction(AbstractSpell spell) {
        return new PatternAction(PatternActionType.CAST_AND_WAIT, spell, 0);
    }

    private PatternAction castAndWaitAction(AbstractSpell spell, int level) {
        return new PatternAction(PatternActionType.CAST_AND_WAIT, spell, level);
    }

    private PatternAction castDurationAction(int duration, AbstractSpell spell) {
        return new PatternAction(PatternActionType.CAST_DURATION, duration, spell, 0);
    }

    private PatternAction castDurationAction(int duration, AbstractSpell spell, int level) {
        return new PatternAction(PatternActionType.CAST_DURATION, duration, spell, level);
    }

    // ============================================================
    //  ★★★ 魔法＋待機時間＋詠唱時間＋レベル エントリー ★★★
    // ============================================================

    public static class CastEntry {
        public final AbstractSpell spell;
        public final int beforeDelay;
        public final int delayAfter;
        public final int customCastTime;
        public final int level;
        public boolean beforeDelayScheduled;

        public CastEntry(AbstractSpell spell, int beforeDelay, int delayAfter, int customCastTime, int level) {
            this.spell = spell;
            this.beforeDelay = beforeDelay;
            this.delayAfter = delayAfter;
            this.customCastTime = customCastTime;
            this.level = level;
            this.beforeDelayScheduled = false;
        }

        public CastEntry(AbstractSpell spell, int delayAfter, int customCastTime, int level) {
            this(spell, 0, delayAfter, customCastTime, level);
        }

        public CastEntry(AbstractSpell spell, int delayAfter, int customCastTime) {
            this(spell, 0, delayAfter, customCastTime, 0);
        }

        public CastEntry(AbstractSpell spell, int delayAfter) {
            this(spell, 0, delayAfter, 0, 0);
        }

        public CastEntry(AbstractSpell spell) {
            this(spell, 0, 0, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell) {
            return new CastEntry(spell, 0, 0, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter) {
            return new CastEntry(spell, 0, delayAfter, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter, int customCastTime) {
            return new CastEntry(spell, 0, delayAfter, customCastTime, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter, int customCastTime, int level) {
            return new CastEntry(spell, 0, delayAfter, customCastTime, level);
        }

        public static CastEntry of(AbstractSpell spell, int beforeDelay, int delayAfter, int customCastTime, int level) {
            return new CastEntry(spell, beforeDelay, delayAfter, customCastTime, level);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds) {
            return new CastEntry(spell, 0, (int)(delaySeconds * 20), 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds, float castSeconds) {
            return new CastEntry(spell, 0, (int)(delaySeconds * 20), (int)(castSeconds * 20), 0);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds, float castSeconds, int level) {
            return new CastEntry(spell, 0, (int)(delaySeconds * 20), (int)(castSeconds * 20), level);
        }

        public static CastEntry of(AbstractSpell spell, float beforeSeconds, float delaySeconds, float castSeconds, int level) {
            return new CastEntry(spell, (int)(beforeSeconds * 20), (int)(delaySeconds * 20), (int)(castSeconds * 20), level);
        }

        public int getCastTime() {
            return customCastTime > 0 ? customCastTime : (spell != null ? spell.getCastTime(1) : 0);
        }

        public int getLevel() {
            return level > 0 ? level : (spell != null ? spell.getMaxLevel() : 1);
        }
    }

    // ============================================================
    //  ★★★ 行動パターン定義 ★★★
    // ============================================================

    private void initPatterns() {
        // ---- フェーズ1 (体力80%以上) ----
        registerPatternWeighted("phase1_close", 1, 0, 8, 4.0,
                castAndWaitAction(SpellRegistry.SHADOW_SLASH.get(), 1),
                waitAction(50)
        );

        registerPatternWeighted("phase1_close2", 1, 0, 7, 4.0,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.CONE_OF_COLD_SPELL.get(), 2, 20, 3),
                        CastEntry.of(SpellRegistry.FIRE_BREATH_SPELL.get(), 2, 20, 3),
                        CastEntry.of(SpellRegistry.DRAGON_BREATH_SPELL.get(), 2, 20, 3)
                )
        );

        registerPatternWeighted("phase1_close3", 1, 0, 10, 0.3,
                castAndWaitAction(SpellRegistry.THUNDERSTORM_SPELL.get(), 1)
        );

        registerPatternWeighted("phase1_far", 1, 20, 40, 0.2,
                castAndWaitAction(ModSpells.METEOR_FALL.get(), 1),
                waitAction(80)
        );

        registerPatternWeighted("phase1_mid2", 1, 10, 25, 2.0,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), 6, 0, 3),
                        CastEntry.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), 6, 0, 3),
                        CastEntry.of(SpellRegistry.BLOOD_SLASH_SPELL.get(),    6, 0, 5)
                ),
                waitAction(60)
        );

        registerPatternWeighted("phase1_mid3", 1, 10, 25, 2.0,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.FIRE_ARROW_SPELL.get(), 2, 0, 5),
                        CastEntry.of(SpellRegistry.FIRE_ARROW_SPELL.get(), 2, 0, 3),
                        CastEntry.of(SpellRegistry.FIRE_ARROW_SPELL.get(), 2, 0, 3)
                ),
                waitAction(100)
        );

        registerPatternWeighted("phase1_mid4", 1, 10, 25, 2.0,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.ENDER_SHOOTING_STAR.get(), 2, 0, 5)
                ),
                waitAction(100)
        );

        // ---- フェーズ2 (体力60%〜80%) ----
        registerPatternWeighted("phase2_close", 2, 0, 12, 1.5,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1)
                ),
                waitAction(20)
        );

        registerPatternWeighted("phase2_mid", 2, 12, 25, 0.2,
                castAndWaitAction(ModSpells.HEAVENLY_BLAST.get(), 9),
                waitAction(130)
        );

        registerPatternWeighted("phase2_mid", 2, 12, 25, 1.0,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.POISON_ARROW_SPELL.get(), 10, 1, 10),
                        CastEntry.of(SpellRegistry.FIRE_ARROW_SPELL.get(), 10, 1, 10),
                        CastEntry.of(SpellRegistry.MAGIC_ARROW_SPELL.get(), 10, 1, 10),
                        CastEntry.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), 10, 1, 10)
                ),
                waitAction(130)
        );

        registerPatternWeighted("phase2_close2", 2, 0, 10, 2.0,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.STARLIGHT.get(), 10, 0, 10),
                        CastEntry.of(ModSpells.STARLIGHT.get(), 10, 0, 10),
                        CastEntry.of(ModSpells.STARLIGHT.get(), 10, 0, 10),
                        CastEntry.of(ModSpells.STARLIGHT.get(), 10, 0, 10)
                ),
                waitAction(40)
        );

        registerPatternWeighted("phase2_close3", 2, 0, 12, 5.0,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.CHAIN_LIGHTNING_SPELL.get(), 2, 40, 1)
                ),
                waitAction(20)
        );

        // ---- フェーズ3 (体力40%〜60%) ----
        registerPattern("phase3_close", 3, 0, 10,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 5, 0, 7)
                ),
                waitAction(80)
        );

        registerPattern("phase3_mid", 3, 10, 20,
                castAndWaitAction(ModSpells.ABSOLUTE_ZERO.get(), 9),
                waitAction(100)
        );

        registerPattern("phase3_close2", 3, 0, 10,
                castAndWaitAction(ModSpells.SEVEN_COLORED_CAGE.get(), 10),
                waitAction(100)
        );

        // ---- フェーズ4 (体力20%〜40%) ----
        registerPattern("phase4_all", 4, 0, 30,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.BLACK_HOLE_SPELL.get(), 5, 10, 8),
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 1, 1, 8),
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 1, 1, 8),
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 1, 1, 8),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(ModSpells.MARK_OF_DETONATION.get(), 2, 0, 1),
                        CastEntry.of(SpellRegistry.STARFALL_SPELL.get(), 2, 40, 5)
                ),
                waitAction(150)
        );

        // ---- フェーズ5 (体力20%未満) ----
        registerPattern("phase5_close", 5, 0, 10,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.STARLIGHT.get(), 2, 5, 9),
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 2, 5, 9)
                ),
                waitAction(50)
        );

        registerPattern("phase5_mid", 5, 10, 25,
                castDurationAction(20, ModSpells.FUNNEL.get(), 4),
                waitAction(100)
        );
    }

    // ============================================================
    //  パターン登録・管理
    // ============================================================

    private void registerPattern(String name, int phase, double minDist, double maxDist, PatternAction... actions) {
        registerPatternWeighted(name, phase, minDist, maxDist, 1.0, actions);
    }

    private void registerPattern(String name, PatternAction... actions) {
        registerPattern(name, 0, -1, -1, actions);
    }

    private void registerPattern(String name, int phase, PatternAction... actions) {
        registerPattern(name, phase, -1, -1, actions);
    }

    private void registerPatternWeighted(String name, int phase, double minDist, double maxDist, double weight, PatternAction... actions) {
        ActionPattern pattern = new ActionPattern(name, phase, minDist, maxDist, weight, actions);
        allPatterns.add(pattern);
        patternMap.put(name, pattern);
    }

    private int getCurrentPhase() {
        double healthRatio = this.getHealth() / this.getMaxHealth();
        if (healthRatio < 0.2) return 5;
        if (healthRatio < 0.4) return 4;
        if (healthRatio < 0.6) return 3;
        if (healthRatio < 0.8) return 2;
        return 1;
    }

    private ActionPattern pickWeighted(List<ActionPattern> candidates) {
        if (candidates.size() == 1) return candidates.get(0);

        double totalWeight = 0;
        for (ActionPattern p : candidates) {
            totalWeight += p.weight;
        }

        if (totalWeight <= 0) {
            return candidates.get(RANDOM.nextInt(candidates.size()));
        }

        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0;
        for (ActionPattern p : candidates) {
            cumulative += p.weight;
            if (roll < cumulative) {
                return p;
            }
        }

        return candidates.get(candidates.size() - 1);
    }

    private void selectRandomPattern() {
        if (allPatterns.isEmpty()) return;

        int currentPhase = getCurrentPhase();
        LivingEntity target = this.getTarget();
        double dist = target != null ? this.distanceTo(target) : -1;

        List<ActionPattern> candidates = new ArrayList<>();
        for (ActionPattern pattern : allPatterns) {
            if (pattern.phase != 0 && pattern.phase != currentPhase) continue;
            if (pattern.minDist > 0 && dist < pattern.minDist) continue;
            if (pattern.maxDist > 0 && dist > pattern.maxDist) continue;
            candidates.add(pattern);
        }

        if (candidates.isEmpty()) {
            candidates = allPatterns;
        }

        ActionPattern selected = pickWeighted(candidates);
        currentPatternName = selected.name;
        currentPattern = selected;
        currentActionIndex = 0;
        actionTimer = 0;
        multiCastStep = 0;
        patternRunning = true;
        patternCooldown = 0;
        waitingForCastComplete = false;
        patternCheckCounter = 0;

        // 選択されたパターンの全 CastEntry の beforeDelayScheduled をリセット
        for (PatternAction action : selected.actions) {
            if (action.castEntries != null) {
                for (CastEntry entry : action.castEntries) {
                    entry.beforeDelayScheduled = false;
                }
            }
        }

        System.out.println("[EternalWizard] Pattern changed to: " + currentPatternName + " (phase=" + currentPhase + ", dist=" + dist + ", weight=" + selected.weight + ")");

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    private void updatePhaseInterval() {
        double healthRatio = this.getHealth() / this.getMaxHealth();
        if (healthRatio < 0.2) {
            phaseInterval = 6;
        } else if (healthRatio < 0.4) {
            phaseInterval = 8;
        } else if (healthRatio < 0.6) {
            phaseInterval = 10;
        } else if (healthRatio < 0.8) {
            phaseInterval = 14;
        } else {
            phaseInterval = 18;
        }
    }

    private void selectPatternByPhase() {
        selectRandomPattern();
    }

    // ============================================================
    //  ★★★ パターン実行エンジン ★★★
    // ============================================================

    private void executePattern() {
        if (this.tickCount % 20 == 0) {
            System.out.println("[EternalWizard] executePattern: patternRunning=" + patternRunning +
                    ", currentPattern=" + (currentPattern != null ? currentPattern.name : "null") +
                    ", target=" + (this.getTarget() != null ? this.getTarget().getName().getString() : "null") +
                    ", actionTimer=" + actionTimer +
                    ", currentActionIndex=" + currentActionIndex +
                    ", waitingForCastComplete=" + waitingForCastComplete);
        }

        if (currentPattern == null || !patternRunning) {
            patternCheckCounter++;
            if (patternCheckCounter >= phaseInterval) {
                patternCheckCounter = 0;
                selectPatternByPhase();
            }
            return;
        }

        if (patternCooldown > 0) {
            patternCooldown--;
            return;
        }

        if (waitingForCastComplete) {
            if (!this.isCasting()) {
                waitingForCastComplete = false;
                currentCastingSpellName = "";
                displayTimer = 0;

                PatternAction currentAction = currentPattern.getAction(currentActionIndex);
                boolean hasMoreEntries = false;
                int nextDelay = 0;

                if (currentAction != null) {
                    if (currentAction.type == PatternActionType.CAST_MULTI_CUSTOM) {
                        if (multiCastStep < currentAction.castEntries.size()) {
                            multiCastStep++;
                            hasMoreEntries = multiCastStep < currentAction.castEntries.size();
                        }
                    } else if (currentAction.type == PatternActionType.CAST_MULTI_INSTANT
                            || currentAction.type == PatternActionType.CAST_MULTI_INTERVAL) {
                        multiCastStep++;
                        hasMoreEntries = multiCastStep < currentAction.spells.size();
                        nextDelay = currentAction.interval > 0 ? currentAction.interval : DEFAULT_MULTI_CAST_GAP;
                    } else if (currentAction.type == PatternActionType.CAST_REPEAT) {
                        multiCastStep++;
                        hasMoreEntries = multiCastStep < currentAction.repeatCount;
                    }
                }

                if (!hasMoreEntries) {
                    currentActionIndex++;
                    multiCastStep = 0;
                } else {
                    actionTimer = nextDelay;
                }
            }
            return;
        }

        PatternAction action = currentPattern.getAction(currentActionIndex);
        if (action == null) {
            patternCooldown = 10;
            selectPatternByPhase();
            return;
        }

        if (actionTimer > 0) {
            actionTimer--;
            if (actionTimer == 0 && pendingAdvance) {
                pendingAdvance = false;
                currentActionIndex++;
                multiCastStep = 0;
            }
            return;
        }

        boolean finished = executeAction(action);
        if (finished) {
            if (action.duration > 0) {
                actionTimer = action.duration;
                pendingAdvance = true;
            } else {
                currentActionIndex++;
                multiCastStep = 0;
            }
        }
    }

    private boolean executeAction(PatternAction action) {
        switch (action.type) {
            case WAIT:
                return true;

            case MOVE:
                doOrbitMove();
                return true;

            case CAST_SPELL:
                if (!action.spells.isEmpty()) {
                    AbstractSpell spell = action.spells.get(0);
                    int level = action.spellLevel > 0 ? action.spellLevel : spell.getMaxLevel();
                    int castTime = spell.getCastTime(1);
                    if (castTime > 0) {
                        Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                        currentCastingSpellName = spell.getDisplayName(player).getString();
                        displayTimer = castTime + 10;
                        castSpellSafely(spell, level);
                        waitingForCastComplete = true;
                        return false;
                    } else {
                        castSpellSafely(spell, level);
                    }
                }
                return true;

            case CAST_MULTI_CUSTOM: {
                if (action.castEntries == null || action.castEntries.isEmpty()) {
                    return true;
                }

                if (multiCastStep >= action.castEntries.size()) {
                    multiCastStep = 0;
                    return true;
                }

                CastEntry entry = action.castEntries.get(multiCastStep);
                if (entry == null) return true;

                if (entry.beforeDelay > 0 && !entry.beforeDelayScheduled) {
                    entry.beforeDelayScheduled = true;
                    actionTimer = entry.beforeDelay;
                    Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                    currentCastingSpellName = entry.spell.getDisplayName(player).getString();
                    displayTimer = entry.beforeDelay + entry.getCastTime() + 10;
                    return false;
                }

                if (entry.beforeDelay > 0 && entry.beforeDelayScheduled && actionTimer > 0) {
                    return false;
                }

                if (entry.spell != null) {
                    int level = entry.getLevel();
                    int castTime = entry.getCastTime();
                    if (castTime > 0) {
                        Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                        currentCastingSpellName = entry.spell.getDisplayName(player).getString();
                        displayTimer = castTime + 10;
                        castSpellSafely(entry.spell, level);

                        if (entry.customCastTime > 0 && this.isCasting()) {
                            MagicData magicData = this.getMagicData();
                            magicData.initiateCast(entry.spell, level, entry.customCastTime,
                                    CastSource.MOB, SpellSelectionManager.MAINHAND);
                        }

                        waitingForCastComplete = true;
                        return false;
                    } else {
                        castSpellSafely(entry.spell, level);
                        multiCastStep++;
                        if (entry.delayAfter > 0) {
                            actionTimer = entry.delayAfter;
                            return false;
                        } else {
                            return true;
                        }
                    }
                }

                multiCastStep++;
                return true;
            }

            case CAST_MULTI_INSTANT:
            case CAST_MULTI_INTERVAL: {
                if (action.spells.isEmpty()) {
                    return true;
                }

                if (multiCastStep >= action.spells.size()) {
                    multiCastStep = 0;
                    return true;
                }

                AbstractSpell spell = action.spells.get(multiCastStep);
                int level = action.spellLevel > 0 ? action.spellLevel : spell.getMaxLevel();
                int castTime = spell.getCastTime(1);
                if (castTime > 0) {
                    Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                    currentCastingSpellName = spell.getDisplayName(player).getString();
                    displayTimer = castTime + 10;
                    castSpellSafely(spell, level);
                    waitingForCastComplete = true;
                    return false;
                } else {
                    castSpellSafely(spell, level);
                }

                multiCastStep++;

                if (multiCastStep >= action.spells.size()) {
                    multiCastStep = 0;
                    return true;
                }

                actionTimer = action.interval > 0 ? action.interval : DEFAULT_MULTI_CAST_GAP;
                return false;
            }

            case CAST_REPEAT: {
                if (action.spells.isEmpty()) {
                    return true;
                }

                if (multiCastStep >= action.repeatCount) {
                    multiCastStep = 0;
                    return true;
                }

                AbstractSpell spell = action.spells.get(0);
                int level = action.spellLevel > 0 ? action.spellLevel : spell.getMaxLevel();
                int castTime = spell.getCastTime(1);
                if (castTime > 0) {
                    Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                    currentCastingSpellName = spell.getDisplayName(player).getString();
                    displayTimer = castTime + 10;
                    castSpellSafely(spell, level);
                    waitingForCastComplete = true;
                    return false;
                } else {
                    castSpellSafely(spell, level);
                }

                multiCastStep++;

                if (multiCastStep >= action.repeatCount) {
                    multiCastStep = 0;
                    return true;
                }

                actionTimer = action.interval > 0 ? action.interval : DEFAULT_MULTI_CAST_GAP;
                return false;
            }

            case CAST_AND_WAIT:
                if (!action.spells.isEmpty()) {
                    AbstractSpell spell = action.spells.get(0);
                    int level = action.spellLevel > 0 ? action.spellLevel : spell.getMaxLevel();
                    Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                    currentCastingSpellName = spell.getDisplayName(player).getString();
                    displayTimer = spell.getCastTime(1) + 10;
                    castSpellSafely(spell, level);
                    waitingForCastComplete = true;
                    return false;
                }
                return true;

            case CAST_DURATION:
                if (!action.spells.isEmpty()) {
                    AbstractSpell spell = action.spells.get(0);
                    int level = action.spellLevel > 0 ? action.spellLevel : spell.getMaxLevel();
                    castSpellSafely(spell, level);
                }
                return true;
        }
        return true;
    }

    // ============================================================
    //  ★★★ 表示更新 ★★★
    // ============================================================

    private void updateDisplay() {
        if (displayTimer > 0) {
            displayTimer--;
            if (displayTimer > 0 && !currentCastingSpellName.isEmpty()) {
                LivingEntity target = this.getTarget();
                if (target instanceof ServerPlayer player) {
                    player.displayClientMessage(
                            Component.literal("§e§l⚡ " + currentCastingSpellName + " §7詠唱中..."),
                            true
                    );
                }
            } else {
                currentCastingSpellName = "";
            }
        }
    }

    // ============================================================
    //  ★★★ 周回移動メソッド ★★★
    // ============================================================

    private void doOrbitMove() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        double healthRatio = this.getHealth() / this.getMaxHealth();
        double orbitSpeed = 0.025 + (1.0 - healthRatio) * 0.045;
        orbitAngle += orbitSpeed;

        double orbitRadius = computeOrbitRadius();

        double x = target.getX() + Math.cos(orbitAngle) * orbitRadius;
        double z = target.getZ() + Math.sin(orbitAngle) * orbitRadius;

        double groundY = getGroundLevel(x, z);
        double y = groundY + FLOAT_HEIGHT;
        if (y < target.getY() + 2.0) {
            y = target.getY() + 3.0;
        }

        this.moveControl.setWantedPosition(x, y, z, MOVEMENT_SPEED);
        this.setMoving(true);
    }

    private double computeOrbitRadius() {
        double baseRadius;
        double wobble;

        if (currentPattern != null && currentPattern.minDist > 0 && currentPattern.maxDist > 0) {
            double center = (currentPattern.minDist + currentPattern.maxDist) / 2.0;
            double halfRange = (currentPattern.maxDist - currentPattern.minDist) / 2.0;
            baseRadius = center;
            wobble = Math.min(halfRange * 0.8, halfRange);
        } else if (currentPattern != null && currentPattern.minDist > 0) {
            baseRadius = currentPattern.minDist + 1.5;
            wobble = 1.0;
        } else if (currentPattern != null && currentPattern.maxDist > 0) {
            baseRadius = currentPattern.maxDist - 1.5;
            wobble = 1.0;
        } else {
            baseRadius = 4.75;
            wobble = 1.5;
        }

        return baseRadius + Math.sin(orbitAngle * 0.7) * wobble;
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) return;

        Vec3 targetPos = target.position();
        float targetYaw = target.getYRot();
        double lookX = -Math.sin(Math.toRadians(targetYaw));
        double lookZ = -Math.cos(Math.toRadians(targetYaw));

        double followDistance = 1.5;
        double targetX = targetPos.x - lookX * followDistance;
        double targetZ = targetPos.z - lookZ * followDistance;
        double targetY = targetPos.y + target.getEyeHeight() * 0.5;

        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        double dy = targetY - this.getEyeY();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180F / (float) Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontalDist) * (180F / (float) Math.PI)));

        this.setXRot(pitch % 360);
        this.setYRot(yaw % 360);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    // ============================================================
    //  ★★★ 高さ維持メソッド ★★★
    // ============================================================

    private void maintainHeight() {
        double groundY = getGroundLevel(this.getX(), this.getZ());
        double targetY = groundY + FLOAT_HEIGHT;

        LivingEntity target = this.getTarget();
        if (target != null) {
            if (targetY < target.getY() + 2.0) {
                targetY = target.getY() + 3.0;
            }
        }

        double currentY = this.getY();
        double diff = targetY - currentY;

        if (Math.abs(diff) > 0.1) {
            double moveSpeed = Math.min(Math.abs(diff), 0.2);
            double newY = currentY + Math.signum(diff) * moveSpeed;
            this.setPos(this.getX(), newY, this.getZ());
        }
    }

    // ============================================================
    //  ★★★ キャストメソッド ★★★
    // ============================================================

    private void castSpellSafely(AbstractSpell spell) {
        castSpellSafely(spell, spell.getMaxLevel());
    }

    private void castSpellSafely(AbstractSpell spell, int level) {
        if (spell == null) return;
        if (EXCLUDED_SPELLS.contains(spell.getClass().getSimpleName())) return;
        this.initiateCastSpell(spell, level);
    }

    // ============================================================
    //  ★★★ Goal登録 ★★★
    // ============================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ============================================================
    //  魔法リスト
    // ============================================================

    private static List<AbstractSpell> cachedSpells = null;

    private static List<AbstractSpell> getAllSpells() {
        if (cachedSpells == null) {
            cachedSpells = new ArrayList<>();
            SpellRegistry.REGISTRY.get().forEach(spell -> {
                String className = spell.getClass().getSimpleName();
                if (!EXCLUDED_SPELLS.contains(className)) {
                    cachedSpells.add(spell);
                }
            });
            addCustomSpells(cachedSpells);
        }
        return cachedSpells;
    }

    private static void addCustomSpells(List<AbstractSpell> list) {
        list.add(ModSpells.FLAME_RAY.get());
        list.add(ModSpells.MARK_OF_DETONATION.get());
        list.add(ModSpells.NAPALM_RAIN.get());
        list.add(ModSpells.PHOENIX_BLESSING.get());
        list.add(ModSpells.METEOR_FALL.get());
        list.add(ModSpells.SUMMON_PYROMANCER.get());
        list.add(ModSpells.FROST_ARMOR.get());
        list.add(ModSpells.GLACIAL_EXECUTION.get());
        list.add(ModSpells.ABSOLUTE_ZERO.get());
        list.add(ModSpells.SUMMON_CRYOMANCER.get());
        list.add(ModSpells.RAISEN.get());
        list.add(ModSpells.SOLAR_RAY.get());
        list.add(ModSpells.UNFADING.get());
        list.add(ModSpells.SUMMON_APOTHECARIST.get());
        list.add(ModSpells.HOLY_RAY.get());
        list.add(ModSpells.HEAVENLY_BLAST.get());
        list.add(ModSpells.PROVIDENTIAL_CONDUIT.get());
        list.add(ModSpells.SUMMON_PRIEST.get());
        list.add(ModSpells.ENDER_SHOOTING_STAR.get());
        list.add(ModSpells.FREISCHUTZ.get());
        list.add(ModSpells.VOID_RAY.get());
        list.add(ModSpells.SPECTAL_RAY.get());
        list.add(ModSpells.SUMMON_ARCHEVOKER.get());
        list.add(ModSpells.SOUL_LINK.get());
        list.add(ModSpells.SACRIFICIAL_EDGE.get());
        list.add(ModSpells.FUNNEL.get());
        list.add(ModSpells.POLYCHROMATIC_LANCE.get());
        list.add(ModSpells.POLYCHROMATIC_BEAM.get());
        list.add(ModSpells.SUMMON_WIZARDS.get());
        list.add(ModSpells.STARLIGHT.get());
    }

    // ============================================================
    //  体力しきい値管理
    // ============================================================

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.getEntity() instanceof LivingEntity attacker) {
            this.setTarget(attacker);
        }
        boolean result = super.hurt(pSource, pAmount);

        // ⭐ 距離減衰なしで Wither の被ダメージ音を全プレイヤーに再生
        if (!level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2.0f, 1.0f);
        }

        updatePhaseInterval();
        selectPatternByPhase();
        if (!level().isClientSide) {
            sendBossBarSync(true);
        }
        return result;
    }

    // ============================================================
    //  ★★★ ボスバー（虹色グラデーション）同期 ★★★
    // ============================================================

    private void sendBossBarSync(boolean isHit) {
        float progress = this.getMaxHealth() > 0
                ? Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0f, 1.0f)
                : 0.0f;

        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        this.getX(), this.getY(), this.getZ(),
                        BOSS_BAR_SYNC_RADIUS, this.level().dimension()
                )),
                new PacketBossBarSync(this.getId(), progress, this.getDisplayName(), isHit)
        );
    }

    private void sendBossBarSync() {
        sendBossBarSync(false);
    }

    // ============================================================
    //  ★★★ tick処理 ★★★
    // ============================================================

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);

        if (!level().isClientSide) {
            if (this.getTarget() == null || !this.getTarget().isAlive()) {
                findNearestPlayer();
                if (this.getTarget() == null) {
                    if (this.tickCount % 4 == 0) {
                        sendBossBarSync(false);
                    }
                    return;
                }
            }

            maintainHeight();
            doOrbitMove();

            LivingEntity target = this.getTarget();
            if (target != null) {
                faceTarget(target);
            }

            executePattern();
            updateDisplay();

            if (this.tickCount % 20 == 0) {
                updatePhaseInterval();
            }

            if (this.tickCount % 4 == 0) {
                sendBossBarSync(false);
            }
        }

        if (!level().isClientSide && this.tickCount % 5 == 0) {
            ((ServerLevel) level()).sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX() + (RANDOM.nextDouble() - 0.5) * 1.0,
                    this.getY() + RANDOM.nextDouble() * 2.0,
                    this.getZ() + (RANDOM.nextDouble() - 0.5) * 1.0,
                    2, 0, 0, 0, 0
            );
        }
    }

    // ============================================================
    //  ユーティリティ
    // ============================================================

    private double getGroundLevel(double x, double z) {
        int startY = (int) Math.ceil(this.getY());
        if (startY < -64) startY = -64;

        for (int y = startY; y > -64; y--) {
            BlockPos pos = new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
            if (!level().isEmptyBlock(pos) && !level().getBlockState(pos).isAir()) {
                return y + 1.0;
            }
        }
        return -63.0;
    }

    private void findNearestPlayer() {
        double nearestDist = Double.MAX_VALUE;
        Player nearest = null;

        for (Player player : level().players()) {
            if (!player.isCreative() && !player.isSpectator() && player.isAlive()) {
                double dist = this.distanceToSqr(player);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = player;
                }
            }
        }

        if (nearest != null) {
            this.setTarget(nearest);
        }
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    // ============================================================
    //  属性
    // ============================================================

    public static AttributeSupplier.Builder prepareAttributes() {
        return AbstractSpellCastingMob.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH_VALUE)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(AttributeRegistry.CAST_TIME_REDUCTION.get(), 2.0);
    }

    // ============================================================
    //  サウンド
    // ============================================================

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.ENDER_CAST.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.ENDER_CAST.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        // デフォルトの被ダメージ音は Wither 音で上書きするため、必要に応じて null または空の音を返す
        return SoundRegistry.ENDER_CAST.get();
    }

    @Override
    public void die(DamageSource pDamageSource) {
        super.die(pDamageSource);
        if (!level().isClientSide) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                            this.getX(), this.getY(), this.getZ(),
                            BOSS_BAR_SYNC_RADIUS, this.level().dimension()
                    )),
                    new PacketBossBarSync(this.getId(), 0.0f, this.getDisplayName(), false)
            );
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ============================================================
    //  内部クラス
    // ============================================================

    public enum PatternActionType {
        WAIT,
        MOVE,
        CAST_SPELL,
        CAST_MULTI_INSTANT,
        CAST_MULTI_INTERVAL,
        CAST_MULTI_CUSTOM,
        CAST_REPEAT,
        CAST_AND_WAIT,
        CAST_DURATION
    }

    public static class PatternAction {
        public final PatternActionType type;
        public final int duration;
        public final List<AbstractSpell> spells;
        public final List<CastEntry> castEntries;
        public final int interval;
        public final int repeatCount;
        public final int spellLevel;

        public PatternAction(PatternActionType type, int duration) {
            this(type, duration, new ArrayList<>(), new ArrayList<>(), 0, 0, 0);
        }

        public PatternAction(PatternActionType type, AbstractSpell spell, int level) {
            this(type, 0, spell != null ? List.of(spell) : new ArrayList<>(), new ArrayList<>(), 0, level, 0);
        }

        public PatternAction(PatternActionType type, int duration, AbstractSpell spell, int level) {
            this(type, 0, spell != null ? List.of(spell) : new ArrayList<>(), new ArrayList<>(), 0, level, duration);
        }

        public PatternAction(PatternActionType type, List<AbstractSpell> spells) {
            this(type, 0, spells, new ArrayList<>(), 0, 0, 0);
        }

        public PatternAction(PatternActionType type, List<AbstractSpell> spells, int interval) {
            this(type, 0, spells, new ArrayList<>(), interval, 0, 0);
        }

        public PatternAction(PatternActionType type, CastEntry... entries) {
            this(type, 0, new ArrayList<>(), Arrays.asList(entries), 0, 0, 0);
        }

        private PatternAction(PatternActionType type, int duration, List<AbstractSpell> spells,
                              List<CastEntry> castEntries, int interval, int spellLevel, int repeatCount) {
            this.type = type;
            this.duration = duration;
            this.spells = spells;
            this.castEntries = castEntries;
            this.interval = interval;
            this.spellLevel = spellLevel;
            this.repeatCount = repeatCount;
        }
    }

    public static class ActionPattern {
        public final String name;
        public final int phase;
        public final double minDist;
        public final double maxDist;
        public final double weight;
        public final List<PatternAction> actions;

        public ActionPattern(String name, int phase, double minDist, double maxDist, double weight, PatternAction... actions) {
            this.name = name;
            this.phase = phase;
            this.minDist = minDist;
            this.maxDist = maxDist;
            this.weight = weight > 0 ? weight : 1.0;
            this.actions = Arrays.asList(actions);
        }

        public ActionPattern(String name, int phase, double minDist, double maxDist, PatternAction... actions) {
            this(name, phase, minDist, maxDist, 1.0, actions);
        }

        public PatternAction getAction(int index) {
            if (index < 0 || index >= actions.size()) return null;
            return actions.get(index);
        }
    }

    // ============================================================
    //  飛行制御
    // ============================================================

    static class FlyingMoveControl extends MoveControl {
        private static final double CLOSING_SPEED_MULT = 1.6;
        private static final double RETREATING_SPEED_MULT = 0.5;

        public FlyingMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                this.operation = MoveControl.Operation.WAIT;
                this.mob.setNoGravity(true);

                double dx = this.wantedX - this.mob.getX();
                double dy = this.wantedY - this.mob.getY();
                double dz = this.wantedZ - this.mob.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < 1.0) {
                    this.mob.setDeltaMovement(Vec3.ZERO);
                    return;
                }

                double speedMult = 1.0;
                LivingEntity target = this.mob.getTarget();
                if (target != null) {
                    double currentDistToTarget = this.mob.position().distanceTo(target.position());
                    Vec3 wantedPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
                    double wantedDistToTarget = wantedPos.distanceTo(target.position());

                    if (wantedDistToTarget < currentDistToTarget - 0.05) {
                        speedMult = CLOSING_SPEED_MULT;
                    } else if (wantedDistToTarget > currentDistToTarget + 0.05) {
                        speedMult = RETREATING_SPEED_MULT;
                    }
                }

                double speed = Math.min(this.speedModifier * 0.3 * speedMult, dist * 0.1 * speedMult);
                Vec3 velocity = new Vec3(dx, dy, dz).normalize().scale(speed);

                Vec3 currentVel = this.mob.getDeltaMovement();
                Vec3 newVel = currentVel.add(velocity.subtract(currentVel).scale(0.15));
                this.mob.setDeltaMovement(newVel);
            } else {
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(0.95));
            }
        }
    }
}