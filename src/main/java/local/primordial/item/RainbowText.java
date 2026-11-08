package local.primordial.item;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
public final class RainbowText {
    private RainbowText() {}
    public static MutableComponent animated(Component text) { return at(text.getString(), System.nanoTime() / 1_000_000_000.0); }
    public static MutableComponent at(String text, double seconds) {
        MutableComponent result = Component.empty(); int index = 0;
        for (int point : text.codePoints().toArray()) {
            double hue = seconds / 6 + index * 0.095;
            double brightness = 0.82 + 0.18 * Math.sin(seconds * Math.PI * 1.5 + index * 0.65);
            result.append(Component.literal(new String(Character.toChars(point)))
                .setStyle(Style.EMPTY.withColor(rgb(hue, 0.72, brightness)).withItalic(false)));
            index++;
        }
        return result;
    }
    private static int rgb(double hue, double saturation, double brightness) {
        double h = (hue - Math.floor(hue)) * 6;
        double c = brightness * saturation, x = c * (1 - Math.abs(h % 2 - 1)), m = brightness - c;
        double r = 0, g = 0, b = 0;
        switch ((int)h) {
            case 0 -> {r=c;g=x;} case 1 -> {r=x;g=c;} case 2 -> {g=c;b=x;}
            case 3 -> {g=x;b=c;} case 4 -> {r=x;b=c;} default -> {r=c;b=x;}
        }
        return ((int)Math.round((r+m)*255)<<16) | ((int)Math.round((g+m)*255)<<8) | (int)Math.round((b+m)*255);
    }
}
