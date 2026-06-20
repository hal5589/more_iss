package nekotori_haru.more_iss.util;

public class ColorUtils {

    public static int waveColor(long time, int index, double speed, int startColor, int endColor) {
        double wave = (Math.sin((time / speed) + index) + 1.0) / 2.0;

        int r = (int) (((startColor >> 16) & 0xFF) * (1 - wave) + ((endColor >> 16) & 0xFF) * wave);
        int g = (int) (((startColor >> 8) & 0xFF) * (1 - wave) + ((endColor >> 8) & 0xFF) * wave);
        int b = (int) ((startColor & 0xFF) * (1 - wave) + (endColor & 0xFF) * wave);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int waveColor(long time, double x, double y, double speed, int startColor, int endColor) {
        double phase = (time / speed) + (x * 0.01) + (y * 0.01);
        double wave = (Math.sin(phase) + 1.0) / 2.0;

        int r = (int) (((startColor >> 16) & 0xFF) * (1 - wave) + ((endColor >> 16) & 0xFF) * wave);
        int g = (int) (((startColor >> 8) & 0xFF) * (1 - wave) + ((endColor >> 8) & 0xFF) * wave);
        int b = (int) ((startColor & 0xFF) * (1 - wave) + (endColor & 0xFF) * wave);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int waveGrayWhiteColor(long time, double x, double y, double speed) {
        return waveColor(time, x, y, speed, 0xAAAAAA, 0xFFFFFF);
    }

    public static int waveGrayWhiteColor(long time, int index, double speed) {
        return waveColor(time, index, speed, 0xAAAAAA, 0xFFFFFF);
    }
}