package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import nekotori_haru.more_iss.registry.ModSpells;

public class EternalWizardEntity extends AbstractSpellCastingMob implements Enemy {

    private static final Random RANDOM = new Random();
    private static final double FLOAT_HEIGHT = 5.0;
    private static final double MAX_HEALTH_VALUE = 7777.0;
    private static final double MOVEMENT_SPEED = 0.45;

    private static final int DEFAULT_MULTI_CAST_GAP = 6;

    // ========== 行動パターン管理 ==========
    private ActionPattern currentPattern;
    private int currentActionIndex = 0;
    private int actionTimer = 0;
    private boolean patternRunning = false;
    private int phaseInterval = 20;
    private int patternCooldown = 0;
    private boolean waitingForCastComplete = false;

    private int multiCastStep = 0;
    private int currentSpellCount = 1;
    private boolean isMoving = false;
    private int patternCheckCounter = 0;

    // ⭐ 表示用
    private String currentCastingSpellName = "";
    private int displayTimer = 0;

    // ⭐ 周回移動用
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
        public final int delayAfter;
        public final int customCastTime;
        public final int level;

        public CastEntry(AbstractSpell spell, int delayAfter, int customCastTime, int level) {
            this.spell = spell;
            this.delayAfter = delayAfter;
            this.customCastTime = customCastTime;
            this.level = level;
        }

        public CastEntry(AbstractSpell spell, int delayAfter, int customCastTime) {
            this(spell, delayAfter, customCastTime, 0);
        }

        public CastEntry(AbstractSpell spell, int delayAfter) {
            this(spell, delayAfter, 0, 0);
        }

        public CastEntry(AbstractSpell spell) {
            this(spell, 0, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell) {
            return new CastEntry(spell, 0, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter) {
            return new CastEntry(spell, delayAfter, 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter, int customCastTime) {
            return new CastEntry(spell, delayAfter, customCastTime, 0);
        }

        public static CastEntry of(AbstractSpell spell, int delayAfter, int customCastTime, int level) {
            return new CastEntry(spell, delayAfter, customCastTime, level);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds) {
            return new CastEntry(spell, (int)(delaySeconds * 20), 0, 0);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds, float castSeconds) {
            return new CastEntry(spell, (int)(delaySeconds * 20), (int)(castSeconds * 20), 0);
        }

        public static CastEntry of(AbstractSpell spell, float delaySeconds, float castSeconds, int level) {
            return new CastEntry(spell, (int)(delaySeconds * 20), (int)(castSeconds * 20), level);
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
        // ⭐ 発動確率の係数(weight)を指定したい場合は registerPatternWeighted を使う
        // 例: registerPatternWeighted("name", phase, minDist, maxDist, 2.0, actions...)
        //   → 同じ抽選グループ内でweight=1のパターンより2倍選ばれやすくなる
        //   （weightを省略した registerPattern は常に weight=1.0 として扱われる）

        // ---- フェーズ1 (体力80%以上) ----
        registerPattern("phase1_close", 1, 0, 7,
                castAndWaitAction(SpellRegistry.SHADOW_SLASH.get(), 1)
        );

        registerPattern("phase1_close2", 1, 0, 5,
                castAndWaitAction(SpellRegistry.FIRE_BREATH_SPELL.get(), 2)
        );

        registerPattern("phase1_close3", 1, 0, 5,
                castAndWaitAction(SpellRegistry.CONE_OF_COLD_SPELL.get(), 2)
        );

        registerPattern("phase1_close4", 1, 0, 10,
                castAndWaitAction(SpellRegistry.THUNDERSTORM_SPELL.get(), 1)
        );



        registerPattern("phase1_mid", 1, 10, 25,
                castAndWaitAction(ModSpells.METEOR_FALL.get(), 1),
                waitAction(20)
        );

        registerPattern("phase1_mid2", 1, 10, 25,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), 6, 0, 5),
                        CastEntry.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), 6, 0, 5),
                        CastEntry.of(SpellRegistry.BLOOD_SLASH_SPELL.get(),    6, 0, 7)
                ),
                waitAction(20)
        );

        // ---- フェーズ2 (体力60%〜80%) ----
        registerPattern("phase2_close", 2, 0, 12,
                castMultiCustomAction(
                        CastEntry.of(SpellRegistry.FIREBALL_SPELL.get(), 3, 4, 6),
                        CastEntry.of(SpellRegistry.CHAIN_LIGHTNING_SPELL.get(), 3, 4, 6)
                ),
                waitAction(10)
        );

        registerPattern("phase2_mid", 2, 12, 25,
                castAndWaitAction(ModSpells.HEAVENLY_BLAST.get(), 9),
                waitAction(25)
        );

        // ---- フェーズ3 (体力40%〜60%) ----
        registerPattern("phase3_close", 3, 0, 10,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 5, 8, 7)
                ),
                waitAction(15)
        );

        registerPattern("phase3_mid", 3, 10, 20,
                castAndWaitAction(ModSpells.ABSOLUTE_ZERO.get(), 9),
                waitAction(30)
        );

        // ---- フェーズ4 (体力20%〜40%) ----
        registerPattern("phase4_all", 4, 0, 30,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 3, 6, 8),
                        CastEntry.of(ModSpells.POLYCHROMATIC_BEAM.get(), 3, 6, 8),
                        CastEntry.of(SpellRegistry.BLACK_HOLE_SPELL.get(), 5, 10, 8)
                ),
                waitAction(15)
        );

        // ---- フェーズ5 (体力20%未満) ----
        registerPattern("phase5_close", 5, 0, 10,
                castMultiCustomAction(
                        CastEntry.of(ModSpells.STARLIGHT.get(), 2, 5, 9),
                        CastEntry.of(ModSpells.POLYCHROMATIC_LANCE.get(), 2, 5, 9)
                ),
                waitAction(10)
        );

        registerPattern("phase5_mid", 5, 10, 25,
                castAndWaitAction(ModSpells.HEAVENLY_BLAST.get(), 10),
                castDurationAction(20, ModSpells.FUNNEL.get(), 9),
                waitAction(30)
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

    // ⭐ 発動確率の係数(weight)を指定して登録する版
    // 例: weight=1,1,2 のパターンが同じ抽選グループにいる場合 → 25%, 25%, 50%
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

    /**
     * weight（係数）に応じた重み付き抽選を行う。
     * 例: 候補のweightが [1, 1, 2] の場合 → それぞれ 25%, 25%, 50% の確率で選ばれる。
     */
    private ActionPattern pickWeighted(List<ActionPattern> candidates) {
        if (candidates.size() == 1) return candidates.get(0);

        double totalWeight = 0;
        for (ActionPattern p : candidates) {
            totalWeight += p.weight;
        }

        if (totalWeight <= 0) {
            // 不正な重み合計の場合は均等抽選にフォールバック
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

        // 浮動小数誤差で漏れた場合のフォールバック
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
            // ⭐ 詠唱中もパターンの距離を保ちつつ周回を続ける
            doOrbitMove();

            if (!this.isCasting()) {
                waitingForCastComplete = false;
                currentCastingSpellName = "";
                displayTimer = 0;
                currentActionIndex++;
                multiCastStep = 0;
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
            // ⭐ MOVEアクションの待機中も毎tick周回移動を更新し続ける
            if (action.type == PatternActionType.MOVE) {
                doOrbitMove();
            }
            return;
        }

        boolean finished = executeAction(action);
        if (finished) {
            actionTimer = action.duration;
            currentActionIndex++;
            multiCastStep = 0;
        }
    }

    private boolean executeAction(PatternAction action) {
        switch (action.type) {
            case WAIT:
                return true;

            case MOVE:
                // ⭐ 移動アクションは周回移動に変更
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
                if (entry.spell != null) {
                    int level = entry.getLevel();
                    int castTime = entry.getCastTime();
                    if (castTime > 0) {
                        Player player = this.getTarget() instanceof Player ? (Player) this.getTarget() : null;
                        currentCastingSpellName = entry.spell.getDisplayName(player).getString();
                        displayTimer = castTime + 10;
                        castSpellSafely(entry.spell, level);
                        waitingForCastComplete = true;
                        return false;
                    } else {
                        castSpellSafely(entry.spell, level);
                    }
                    System.out.println("[EternalWizard] Casting: " + entry.spell.getClass().getSimpleName() + " (Lv." + level + ")");
                }

                multiCastStep++;

                if (multiCastStep >= action.castEntries.size()) {
                    multiCastStep = 0;
                    return true;
                }

                actionTimer = Math.max(1, entry.delayAfter);
                return false;
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

        // 1. 周回角度を更新（体力が減るほど速くなる）
        double healthRatio = this.getHealth() / this.getMaxHealth();
        double orbitSpeed = 0.025 + (1.0 - healthRatio) * 0.045; // 0.025〜0.07（高速化）
        orbitAngle += orbitSpeed;

        // 2. 周回半径：現在のパターンに距離設定があればその範囲内、なければデフォルト(4〜5.5)
        double orbitRadius = computeOrbitRadius();

        // 3. ターゲットの周囲の位置を計算
        double x = target.getX() + Math.cos(orbitAngle) * orbitRadius;
        double z = target.getZ() + Math.sin(orbitAngle) * orbitRadius;

        // 4. Y座標：地面から5ブロック、かつターゲットより低すぎないように
        double groundY = getGroundLevel(x, z);
        double y = groundY + FLOAT_HEIGHT;
        if (y < target.getY() + 2.0) {
            y = target.getY() + 3.0;
        }

        // 5. 移動目標を設定
        this.moveControl.setWantedPosition(x, y, z, MOVEMENT_SPEED);
        this.setMoving(true);

        // 6. デバッグ用（必要に応じて削除）
        // System.out.println("[EternalWizard] Orbit: angle=" + orbitAngle + ", radius=" + orbitRadius);
    }

    /**
     * 現在のパターンに minDist/maxDist が設定されている場合、その範囲の中心を基準に
     * ±15%ほど揺らした半径を返す。設定がない場合は従来のデフォルト挙動(4.0〜5.5)。
     */
    private double computeOrbitRadius() {
        double baseRadius;
        double wobble;

        if (currentPattern != null && currentPattern.minDist > 0 && currentPattern.maxDist > 0) {
            double center = (currentPattern.minDist + currentPattern.maxDist) / 2.0;
            double halfRange = (currentPattern.maxDist - currentPattern.minDist) / 2.0;
            baseRadius = center;
            wobble = Math.min(halfRange * 0.8, halfRange); // 範囲を超えないよう余裕を持たせる
        } else if (currentPattern != null && currentPattern.minDist > 0) {
            // 下限のみ指定：下限より少し外側を周回
            baseRadius = currentPattern.minDist + 1.5;
            wobble = 1.0;
        } else if (currentPattern != null && currentPattern.maxDist > 0) {
            // 上限のみ指定：上限より少し内側を周回
            baseRadius = currentPattern.maxDist - 1.5;
            wobble = 1.0;
        } else {
            // 距離指定なし：従来のデフォルト
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
        // ⭐ 常に地面から5ブロックの高さを維持
        double groundY = getGroundLevel(this.getX(), this.getZ());
        double targetY = groundY + FLOAT_HEIGHT;

        // ターゲットがいる場合はターゲットの高さも考慮
        LivingEntity target = this.getTarget();
        if (target != null) {
            if (targetY < target.getY() + 2.0) {
                targetY = target.getY() + 3.0;
            }
        }

        double currentY = this.getY();
        double diff = targetY - currentY;

        // 急激な移動を防ぐため、1tickあたり最大0.2ブロックまで移動
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
        this.initiateCastSpell(spell, Math.min(level, spell.getMaxLevel()));
    }

    // ============================================================
    //  ★★★ Goal登録 ★★★
    // ============================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new SpellBarrageGoal(this, SpellRegistry.FIREBALL_SPELL.get(), 1, 3, 10, 50, 1));
        this.goalSelector.addGoal(6, new WizardAttackGoal(this, 1.5f, 30, 80)
                .setSpells(
                        getAllSpells(),
                        getAllSpells(),
                        getAllSpells(),
                        getAllSpells()
                )
                .setSpellQuality(0.3f, 0.8f)
                .setDrinksPotions()
        );
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
        updatePhaseInterval();
        selectPatternByPhase();
        return result;
    }

    // ============================================================
    //  ★★★ tick処理 ★★★
    // ============================================================

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);

        if (!level().isClientSide) {
            // ----- 1. ターゲットがいる場合のみ動作 -----
            if (this.getTarget() == null || !this.getTarget().isAlive()) {
                findNearestPlayer();
                // ターゲットがいない場合は何もしない
                if (this.getTarget() == null) {
                    return;
                }
            }

            // ----- 2. 高さ維持（常時） -----
            maintainHeight();

            // ----- 3. プレイヤーを向く -----
            LivingEntity target = this.getTarget();
            if (target != null) {
                faceTarget(target);
            }

            // ----- 4. パターン実行 -----
            executePattern();

            // ----- 5. 表示更新 -----
            updateDisplay();

            // ----- 6. フェーズ更新（定期） -----
            if (this.tickCount % 20 == 0) {
                updatePhaseInterval();
            }
        }

        // ----- 7. パーティクル（常時） -----
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
        return SoundRegistry.ENDER_CAST.get();
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
        private static final double CLOSING_SPEED_MULT = 1.6;   // 接近時は速く
        private static final double RETREATING_SPEED_MULT = 0.5; // 離脱時は遅く

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

                // ⭐ ターゲット（プレイヤー）との距離が縮まる移動か、広がる移動かを判定
                double speedMult = 1.0;
                LivingEntity target = this.mob.getTarget();
                if (target != null) {
                    double currentDistToTarget = this.mob.position().distanceTo(target.position());
                    Vec3 wantedPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
                    double wantedDistToTarget = wantedPos.distanceTo(target.position());

                    if (wantedDistToTarget < currentDistToTarget - 0.05) {
                        speedMult = CLOSING_SPEED_MULT;    // 近づこうとしている
                    } else if (wantedDistToTarget > currentDistToTarget + 0.05) {
                        speedMult = RETREATING_SPEED_MULT; // 離れようとしている
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