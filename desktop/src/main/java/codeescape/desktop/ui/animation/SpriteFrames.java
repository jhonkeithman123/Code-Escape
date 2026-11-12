package codeescape.desktop.ui.animation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public final class SpriteFrames {
    private SpriteFrames() {}

    public static BufferedImage[] loadIsolatedWalkFrames(Function<String, BufferedImage> load, String file) {
        if (file == null || !file.matches("modern_(boy|girl)_(walk(_v[1-4])?|run_v[1-4])\\.png")) return null;
        BufferedImage source = load.apply(file);
        if (source == null) return null;
        final int count;
        if (source.getWidth() == 2240 && source.getHeight() == 380) count = 8;
        else if (source.getWidth() == 384 && (source.getHeight() == 365 || source.getHeight() == 390)) count = 4;
        else return null;
        final int cellWidth = source.getWidth() / count;
        Rectangle[] bounds = new Rectangle[count];
        int canvasWidth = 1;
        int canvasHeight = 1;

        for (int i = 0; i < count; i++) {
            int minX = cellWidth, minY = source.getHeight();
            int maxX = -1, maxY = -1;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = i * cellWidth; x < (i + 1) * cellWidth; x++) {
                    if ((source.getRGB(x, y) >>> 24) > 8) {
                        minX = Math.min(minX, x - i * cellWidth);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x - i * cellWidth);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) return null;
            bounds[i] = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            canvasWidth = Math.max(canvasWidth, bounds[i].width);
            canvasHeight = Math.max(canvasHeight, bounds[i].height);
        }

        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            Rectangle b = bounds[i];
            BufferedImage frameImage = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D fg = frameImage.createGraphics();
            fg.setComposite(AlphaComposite.Src);
            fg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            BufferedImage crop = source.getSubimage(i * cellWidth + b.x, b.y, b.width, b.height);
            int dx = (canvasWidth - b.width) / 2;
            int dy = canvasHeight - b.height;
            fg.drawImage(crop, dx, dy, null);
            fg.dispose();
            frames[i] = frameImage;
        }
        return frames;
    }

    public static int detectFrameCount(BufferedImage image) {
        if (image == null) return 1;
        if (image.getWidth() == 2240) return 8;
        if (image.getWidth() == 384 && (image.getHeight() == 365 || image.getHeight() == 390)) return 4;
        if (image.getWidth() == 640 && image.getHeight() == 400) return 4;
        if (image.getWidth() == 173 || image.getWidth() == 171 || image.getWidth() == 229 || image.getWidth() == 740) {
            return image.getWidth() >= 220 ? 4 : 3;
        }
        if (image.getWidth() % 4 == 0) return 4;
        if (image.getWidth() % 3 == 0) return 3;
        return 1;
    }

    public static int[] frameSequenceFor(int count) {
        if (count <= 1) return new int[]{0};
        int[] sequence = new int[count];
        for (int i = 0; i < count; i++) sequence[i] = i;
        return sequence;
    }
}
