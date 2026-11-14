import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class GenIco {
    static BufferedImage resize(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double r = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * r));
        int h = Math.max(1, (int) Math.round(src.getHeight() * r));
        int x = (size - w) / 2;
        int y = (size - h) / 2;
        g.drawImage(src, x, y, w, h, null);
        g.dispose();
        return out;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage logo = ImageIO.read(new File(args[0]));
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        File outFile = new File(args[1]);
        outFile.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            // ICONDIR
            fos.write(0); fos.write(0); // reserved
            fos.write(1); fos.write(0); // type = icon
            fos.write((byte) (sizes.length & 0xFF)); fos.write((byte) ((sizes.length >> 8) & 0xFF));
            ByteArrayOutputStream images = new ByteArrayOutputStream();
            int offset = 6 + 16 * sizes.length;
            for (int size : sizes) {
                BufferedImage img = resize(logo, size);
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                ImageIO.write(img, "png", pngOut);
                byte[] pngBytes = pngOut.toByteArray();
                int w = (size >= 256) ? 0 : size;
                int h = (size >= 256) ? 0 : size;
                fos.write(w);
                fos.write(h);
                fos.write(0); // color count
                fos.write(0); // reserved
                fos.write(1); fos.write(0); // planes
                fos.write(32); fos.write(0); // bit count
                fos.write(pngBytes.length & 0xFF);
                fos.write((pngBytes.length >> 8) & 0xFF);
                fos.write((pngBytes.length >> 16) & 0xFF);
                fos.write((pngBytes.length >> 24) & 0xFF);
                fos.write(offset & 0xFF);
                fos.write((offset >> 8) & 0xFF);
                fos.write((offset >> 16) & 0xFF);
                fos.write((offset >> 24) & 0xFF);
                images.write(pngBytes);
                offset += pngBytes.length;
            }
            fos.write(images.toByteArray());
        }
        System.out.println("wrote " + outFile.getAbsolutePath());
    }
}
