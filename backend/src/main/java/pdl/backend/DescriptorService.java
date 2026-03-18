package pdl.backend;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.awt.Color;

@Service
public class DescriptorService {

    public float[] compute(BufferedImage img, String type) {
        return switch (type.toUpperCase()) {
            case "H1D" -> hist1D(img, 9);
            case "H2D" -> hist2D(img, 8, 8);
            case "H3D" -> hist3D(img, 4, 4, 4);
            default -> throw new IllegalArgumentException();
        };
    }

    public float[] hist1D(BufferedImage img, int n) {
        GrayU8 in = ConvertBufferedImage.convertFrom(img, (GrayU8) null);
        float[] h = new float[n]; float s = 0;
        for (int y = 1; y < in.height - 1; y++) {
            for (int x = 1; x < in.width - 1; x++) {
                int gx = in.get(x+1, y) - in.get(x-1, y), gy = in.get(x, y+1) - in.get(x, y-1);
                int mag = Math.abs(gx) + Math.abs(gy);
                if (mag == 0) continue;
                int b = Math.min((int)((Math.atan2(gy, gx) + Math.PI) * n / (2 * Math.PI)), n - 1);
                h[b] += mag; s += mag;
            }
        }
        return norm(h, s);
    }

    public float[] hist2D(BufferedImage img, int bH, int bS) {
        float[] h = new float[bH * bS], hsv = new float[3];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                rgbToHsv(c.getRed(), c.getGreen(), c.getBlue(), hsv);
                h[Math.min((int)(hsv[0]*bH), bH-1) * bS + Math.min((int)(hsv[1]*bS), bS-1)]++;
            }
        }
        return norm(h, img.getWidth() * img.getHeight());
    }

    public float[] hist3D(BufferedImage img, int bR, int bG, int bB) {
        float[] h = new float[bR * bG * bB];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = c.getRed() * bR / 256, g = c.getGreen() * bG / 256, b = c.getBlue() * bB / 256;
                h[r * (bG * bB) + g * bB + b]++;
            }
        }
        return norm(h, img.getWidth() * img.getHeight());
    }

    private void rgbToHsv(int r, int g, int b, float[] out) {
        Color.RGBtoHSB(r, g, b, out);
    }

    private float[] norm(float[] h, float s) {
        if (s > 0) for (int i=0; i<h.length; i++) h[i] /= s;
        return h;
    }
}
