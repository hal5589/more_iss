package nekotori_haru.more_iss.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import nekotori_haru.more_iss.api.SynthesisUpgradeData;
import nekotori_haru.more_iss.item.ringofsynthesis.RingOfSynthesisItem;
import nekotori_haru.more_iss.util.RenderUtils;

@Mixin(Font.class)
public abstract class FontMixin {

    @Unique
    private static final String WAVE_MARKER = RenderUtils.WAVE_MARKER;

    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void onDrawString(String text, float x, float y, int color, boolean dropShadow,
                              Matrix4f matrix, MultiBufferSource bufferSource,
                              Font.DisplayMode displayMode, int packedLight,
                              int backgroundColor, boolean p_273022_,
                              CallbackInfoReturnable<Integer> cir) {
        if (text != null && text.contains(WAVE_MARKER)) {
            String cleanText = text.replace(WAVE_MARKER, "");
            handleWavingText(cleanText, x, y, dropShadow, matrix, bufferSource);
            cir.setReturnValue(cleanText.length());
            cir.cancel();
            return;
        }

        if (shouldWaveForHotbarItem(text)) {
            handleWavingText(text, x, y, dropShadow, matrix, bufferSource);
            cir.setReturnValue(text.length());
            cir.cancel();
        }
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"), cancellable = true)
    private void onDrawFormatted(FormattedCharSequence sequence, float x, float y, int color, boolean dropShadow,
                                 Matrix4f matrix, MultiBufferSource bufferSource,
                                 Font.DisplayMode displayMode, int packedLight, int backgroundColor,
                                 CallbackInfoReturnable<Integer> cir) {
        if (sequence != null) {
            String text = formatSeqToString(sequence);
            if (text.contains(WAVE_MARKER)) {
                String cleanText = text.replace(WAVE_MARKER, "");
                handleWavingText(cleanText, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(cleanText.length());
                cir.cancel();
                return;
            }
            if (shouldWaveForHotbarItem(text)) {
                handleWavingText(text, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(text.length());
                cir.cancel();
            }
        }
    }

    @Inject(method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"), cancellable = true)
    private void onDrawInternalFormatted(FormattedCharSequence sequence, float x, float y, int color, boolean dropShadow,
                                         Matrix4f matrix, MultiBufferSource bufferSource,
                                         Font.DisplayMode displayMode, int packedLight, int backgroundColor,
                                         CallbackInfoReturnable<Integer> cir) {
        if (sequence != null) {
            String text = formatSeqToString(sequence);
            if (text.contains(WAVE_MARKER)) {
                String cleanText = text.replace(WAVE_MARKER, "");
                handleWavingText(cleanText, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(cleanText.length());
                cir.cancel();
                return;
            }
            if (shouldWaveForHotbarItem(text)) {
                handleWavingText(text, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(text.length());
                cir.cancel();
            }
        }
    }

    @Inject(method = "renderText(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F",
            at = @At("HEAD"), cancellable = true)
    private void onRenderTextFormatted(FormattedCharSequence sequence, float x, float y, int color, boolean dropShadow,
                                       Matrix4f matrix, MultiBufferSource bufferSource,
                                       Font.DisplayMode displayMode, int packedLight, int backgroundColor,
                                       CallbackInfoReturnable<Float> cir) {
        if (sequence != null) {
            String text = formatSeqToString(sequence);
            if (text.contains(WAVE_MARKER)) {
                String cleanText = text.replace(WAVE_MARKER, "");
                handleWavingText(cleanText, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(1.0f);
                cir.cancel();
                return;
            }
            if (shouldWaveForHotbarItem(text)) {
                handleWavingText(text, x, y, dropShadow, matrix, bufferSource);
                cir.setReturnValue(1.0f);
                cir.cancel();
            }
        }
    }

    @Unique
    private boolean shouldWaveForHotbarItem(String text) {
        if (text == null || text.isEmpty()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (mc.screen != null) return false;

        ItemStack selected = mc.player.getInventory().getSelected();
        if (selected.isEmpty()) return false;

        String itemName = selected.getHoverName().getString();

        if (selected.getItem() instanceof RingOfSynthesisItem) {
            return text.equals(itemName) || text.contains(itemName);
        }

        if (SynthesisUpgradeData.isUpgraded(selected)) {
            return text.equals(itemName) || text.contains(itemName);
        }

        return false;
    }

    @Unique
    private void handleWavingText(String str, float x, float y, boolean dropShadow,
                                  Matrix4f matrix, MultiBufferSource bufferSource) {
        Font font = (Font) (Object) this;
        PoseStack poseStack = new PoseStack();
        poseStack.mulPoseMatrix(matrix);

        // ⭐ 修正点: BufferSource 以外の場合は通常描画にフォールバック
        if (!(bufferSource instanceof MultiBufferSource.BufferSource)) {
            font.drawInBatch(str, x, y, 0xFFFFFF, dropShadow, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0, false);
            return;
        }

        MultiBufferSource.BufferSource buffer = (MultiBufferSource.BufferSource) bufferSource;

        float wavespeed = Math.max(3.0f, 7.0f - (str.length() * 0.1f));

        RenderUtils.renderWavingTextDirect(
                font,
                str,
                x,
                y,
                System.nanoTime() * 0.000000001f,
                wavespeed,
                1.0f,
                poseStack,
                buffer,
                dropShadow
        );
    }

    @Unique
    private String formatSeqToString(FormattedCharSequence sequence) {
        StringBuilder sb = new StringBuilder();
        sequence.accept((i, style, codepoint) -> {
            sb.appendCodePoint(codepoint);
            return true;
        });
        return sb.toString();
    }
}