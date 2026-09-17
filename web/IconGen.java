import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Иконки PWA. Запускается один раз, результат коммитится в web/. */
public class IconGen {
    public static void main(String[] args) throws Exception {
        write(192);
        write(512);
    }

    private static void write(int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x1b2a30));
        g.fillRect(0, 0, size, size);

        double cx = size / 2.0, cy = size / 2.0, r = size * 0.34;
        Path2D.Double hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
        }
        hex.closePath();

        g.setColor(new Color(0x4f9d4f));
        g.fill(hex);
        g.setColor(new Color(0x7fd07f));
        g.setStroke(new BasicStroke(size * 0.02f));
        g.draw(hex);
        g.dispose();

        File out = new File("icon-" + size + ".png");
        ImageIO.write(image, "png", out);
        System.out.println("written: " + out.getName() + " " + out.length() + " bytes");
    }
}
