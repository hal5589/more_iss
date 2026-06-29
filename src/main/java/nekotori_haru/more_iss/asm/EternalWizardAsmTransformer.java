package nekotori_haru.more_iss.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.EnumSet;

/**
 * 生ASMによる EternalWizardEntity のダメージキャップ用 CoreMod。
 *
 * 経緯:
 *  - Mixin(@ModifyVariable, @Overwrite等) は最終的にASMバイトコードへ
 *    コンパイルされるラッパーであり機能的には生ASMと同一だが、
 *    要望により Mixin を経由せず ILaunchPluginService + 生ASM で
 *    直接バイトコードを生成する。
 *
 * やること:
 *  - EternalWizardEntity.class のロード時に、クラスへ新しい
 *    private float trueHealth フィールドと
 *    private boolean trueHealthInitialized フィールドを追加し、
 *    getHealth()/setHealth(float) メソッドの本体を、
 *    cap判定込みの実装に「丸ごと差し替える」。
 *  - 既存のJavaソース側に @Override getHealth/setHealth は置かない
 *    (置くとこのTransformerが上書きする対象が変わってしまうため)。
 *
 * 注意:
 *  - ターゲットクラス名は完全修飾名で固定一致させる
 *    (誤って他クラスを変換しないため)。
 *  - cap値・有効フラグは MoreIssConfig.getDamageCap() /
 *    isDamageCapEnabled() を静的呼び出しするバイトコードを生成する。
 */
public class EternalWizardAsmTransformer implements ILaunchPluginService {

    private static final String NAME = "more_iss_eternal_wizard_asm";

    private static final String TARGET_CLASS_INTERNAL =
            "nekotori_haru/more_iss/entity/EternalWizardEntity";

    private static final String LIVING_ENTITY_INTERNAL =
            "net/minecraft/world/entity/LivingEntity";

    // setHealth/getHealth はMCPマッピング(Mojang名)では setHealth/getHealth のまま。
    // 開発環境では Mojang名、本番(SRG)環境では実行時に ModLauncher が
    // 既にMojang名へリマップされたクラスをロードするため、
    // ここでは一貫して Mojang名で記述してよい。
    private static final String GET_HEALTH_NAME = "getHealth";
    private static final String SET_HEALTH_NAME = "setHealth";

    private static final String CONFIG_CLASS_INTERNAL =
            "nekotori_haru/more_iss/registry/MoreIssConfig";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (TARGET_CLASS_INTERNAL.equals(classType.getInternalName())) {
            return EnumSet.of(Phase.AFTER);
        }
        return EnumSet.noneOf(Phase.class);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        if (!TARGET_CLASS_INTERNAL.equals(classType.getInternalName())) {
            return false;
        }

        boolean modified = false;

        // 1. 専用フィールドを追加
        addFieldIfAbsent(classNode, "more_iss$trueHealth", "F");
        addFieldIfAbsent(classNode, "more_iss$trueHealthInitialized", "Z");
        modified = true;

        // 2. getHealth() をクラスに新規追加(差し替え)
        removeExistingMethod(classNode, GET_HEALTH_NAME, "()F");
        classNode.methods.add(buildGetHealthMethod());

        // 3. setHealth(float) をクラスに新規追加(差し替え)
        removeExistingMethod(classNode, SET_HEALTH_NAME, "(F)V");
        classNode.methods.add(buildSetHealthMethod());

        return modified;
    }

    private void addFieldIfAbsent(ClassNode classNode, String name, String descriptor) {
        boolean exists = classNode.fields.stream()
                .anyMatch(f -> f.name.equals(name));
        if (!exists) {
            FieldNode field = new FieldNode(
                    Opcodes.ACC_PRIVATE,
                    name,
                    descriptor,
                    null,
                    null
            );
            classNode.fields.add(field);
        }
    }

    private void removeExistingMethod(ClassNode classNode, String name, String descriptor) {
        classNode.methods.removeIf(m -> m.name.equals(name) && m.desc.equals(descriptor));
    }

    /**
     * 生成する getHealth() の等価Javaソース:
     *
     * public float getHealth() {
     *     if (!this.more_iss$trueHealthInitialized) {
     *         this.more_iss$trueHealth = super.getHealth();
     *         this.more_iss$trueHealthInitialized = true;
     *     }
     *     return this.more_iss$trueHealth;
     * }
     */
    private MethodNode buildGetHealthMethod() {
        MethodNode mv = new MethodNode(
                Opcodes.ACC_PUBLIC,
                GET_HEALTH_NAME,
                "()F",
                null,
                null
        );

        InsnList code = mv.instructions;
        LabelNode skipInit = new LabelNode();

        // if (this.more_iss$trueHealthInitialized) goto skipInit;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealthInitialized", "Z"));
        code.add(new JumpInsnNode(Opcodes.IFNE, skipInit));

        // this.more_iss$trueHealth = super.getHealth();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, LIVING_ENTITY_INTERNAL,
                GET_HEALTH_NAME, "()F", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));

        // this.more_iss$trueHealthInitialized = true;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealthInitialized", "Z"));

        code.add(skipInit);

        // return this.more_iss$trueHealth;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));
        code.add(new InsnNode(Opcodes.FRETURN));

        mv.maxStack = 2;
        mv.maxLocals = 1;
        return mv;
    }

    /**
     * 生成する setHealth(float) の等価Javaソース:
     *
     * public void setHealth(float health) {
     *     float maxHealth = this.getMaxHealth();
     *     float clamped = Mth.clamp(health, 0.0F, maxHealth);
     *
     *     if (this.more_iss$trueHealthInitialized
     *             && MoreIssConfig.isDamageCapEnabled()) {
     *         float cap = MoreIssConfig.getDamageCap();
     *         float decrease = this.more_iss$trueHealth - clamped;
     *         boolean lethal = clamped <= 0.0F;
     *         if (decrease > cap && !lethal) {
     *             clamped = this.more_iss$trueHealth - cap;
     *         }
     *     }
     *
     *     this.more_iss$trueHealth = clamped;
     *     this.more_iss$trueHealthInitialized = true;
     *     super.setHealth(this.more_iss$trueHealth);
     * }
     *
     * ローカル変数スロット割り当て:
     *   0: this
     *   1: health (引数)
     *   2: maxHealth
     *   3: clamped
     *   4: cap
     *   5: decrease
     *   6: lethal (boolean, 0/1で扱う)
     */
    private MethodNode buildSetHealthMethod() {
        MethodNode mv = new MethodNode(
                Opcodes.ACC_PUBLIC,
                SET_HEALTH_NAME,
                "(F)V",
                null,
                null
        );

        InsnList code = mv.instructions;

        // float maxHealth = this.getMaxHealth();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET_CLASS_INTERNAL,
                "getMaxHealth", "()F", false));
        code.add(new VarInsnNode(Opcodes.FSTORE, 2));

        // float clamped = Mth.clamp(health, 0.0F, maxHealth);
        code.add(new VarInsnNode(Opcodes.FLOAD, 1));
        code.add(new InsnNode(Opcodes.FCONST_0));
        code.add(new VarInsnNode(Opcodes.FLOAD, 2));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/util/Mth",
                "clamp", "(FFF)F", false));
        code.add(new VarInsnNode(Opcodes.FSTORE, 3));

        LabelNode afterCapBlock = new LabelNode();

        // if (!this.more_iss$trueHealthInitialized) goto afterCapBlock;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealthInitialized", "Z"));
        code.add(new JumpInsnNode(Opcodes.IFEQ, afterCapBlock));

        // if (!MoreIssConfig.isDamageCapEnabled()) goto afterCapBlock;
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONFIG_CLASS_INTERNAL,
                "isDamageCapEnabled", "()Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, afterCapBlock));

        // float cap = MoreIssConfig.getDamageCap();
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONFIG_CLASS_INTERNAL,
                "getDamageCap", "()F", false));
        code.add(new VarInsnNode(Opcodes.FSTORE, 4));

        // float decrease = this.more_iss$trueHealth - clamped;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));
        code.add(new VarInsnNode(Opcodes.FLOAD, 3));
        code.add(new InsnNode(Opcodes.FSUB));
        code.add(new VarInsnNode(Opcodes.FSTORE, 5));

        // boolean lethal = clamped <= 0.0F;  (lethal を 6番スロットに int 0/1 で格納)
        LabelNode lethalTrue = new LabelNode();
        LabelNode lethalDone = new LabelNode();
        code.add(new VarInsnNode(Opcodes.FLOAD, 3));
        code.add(new InsnNode(Opcodes.FCONST_0));
        code.add(new InsnNode(Opcodes.FCMPG)); // clamped - 0.0 -> 結果<=0ならIFLEで真
        code.add(new JumpInsnNode(Opcodes.IFLE, lethalTrue));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new VarInsnNode(Opcodes.ISTORE, 6));
        code.add(new JumpInsnNode(Opcodes.GOTO, lethalDone));
        code.add(lethalTrue);
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new VarInsnNode(Opcodes.ISTORE, 6));
        code.add(lethalDone);

        // if (decrease > cap && !lethal) { clamped = trueHealth - cap; }
        LabelNode skipClampOverride = new LabelNode();
        code.add(new VarInsnNode(Opcodes.FLOAD, 5)); // decrease
        code.add(new VarInsnNode(Opcodes.FLOAD, 4)); // cap
        code.add(new InsnNode(Opcodes.FCMPG));
        code.add(new JumpInsnNode(Opcodes.IFLE, skipClampOverride)); // decrease <= cap -> skip

        code.add(new VarInsnNode(Opcodes.ILOAD, 6)); // lethal
        code.add(new JumpInsnNode(Opcodes.IFNE, skipClampOverride)); // lethal==true -> skip

        // clamped = this.more_iss$trueHealth - cap;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));
        code.add(new VarInsnNode(Opcodes.FLOAD, 4));
        code.add(new InsnNode(Opcodes.FSUB));
        code.add(new VarInsnNode(Opcodes.FSTORE, 3));

        code.add(skipClampOverride);
        code.add(afterCapBlock);

        // this.more_iss$trueHealth = clamped;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.FLOAD, 3));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));

        // this.more_iss$trueHealthInitialized = true;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealthInitialized", "Z"));

        // super.setHealth(this.more_iss$trueHealth);
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_CLASS_INTERNAL,
                "more_iss$trueHealth", "F"));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, LIVING_ENTITY_INTERNAL,
                SET_HEALTH_NAME, "(F)V", false));

        code.add(new InsnNode(Opcodes.RETURN));

        mv.maxStack = 3;
        mv.maxLocals = 7;
        return mv;
    }
}