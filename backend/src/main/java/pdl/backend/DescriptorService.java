package pdl.backend;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class DescriptorService {

    public float[] hist1D(byte[] data, int nBins) {
        BufferedImage buffered;
        try {
            buffered = ImageIO.read(new ByteArrayInputStream(data));
            if (buffered == null) {
                throw new IllegalArgumentException("Impossible de décoder l’image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur de décodage image", e);
        }

        GrayU8 input = ConvertBufferedImage.convertFrom(buffered, (GrayU8) null);

        int[] histInt = new int[nBins];

        for (int y = 1; y < input.getHeight() - 1; y++) {
            for (int x = 1; x < input.getWidth() - 1; x++) {

                int gx = input.get(x + 1, y) - input.get(x - 1, y);
                int gy = input.get(x, y + 1) - input.get(x, y - 1);
                int magnitude = Math.abs(gx) + Math.abs(gy);
                if (magnitude == 0) continue;
                double angle = Math.atan2(gy, gx);

                if (angle < 0) angle += 2 * Math.PI;

                int bin = (int) (angle * nBins / (2 * Math.PI));
                if (bin >= nBins) bin = nBins - 1;

                histInt[bin] += magnitude;
            }
        }

        float[] hist = new float[nBins];
        double sum = 0.0;
        for (int v : histInt) sum += v;
        if (sum == 0) {
            return hist;
        }
        for (int i = 0; i < nBins; i++) {
            hist[i] = (float) (histInt[i] / sum);
        }
        return hist;
    }

    private static void rgbToHsv(int r, int g, int b, float[] out) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h, s, v = max;

        if (delta == 0) {
            h = 0;
        } else if (max == rf) {
            h = 60f * (((gf - bf) / delta) % 6f);
        } else if (max == gf) {
            h = 60f * (((bf - rf) / delta) + 2f);
        } else {
            h = 60f * (((rf - gf) / delta) + 4f);
        }
        if (h < 0) h += 360f;

        s = (max == 0) ? 0 : (delta / max);

        out[0] = h / 360f; // H dans [0,1]
        out[1] = s;        // S dans [0,1]
        out[2] = v;        // V dans [0,1]
    }

    public float[] hist2D(byte[] data, int binsH, int binsS) {
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                throw new IllegalArgumentException("Impossible de décoder l’image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur de décodage image", e);
        }

        int w = img.getWidth();
        int h = img.getHeight();

        int size = binsH * binsS;
        float[] hist = new float[size];

        float[] hsv = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                rgbToHsv(r, g, b, hsv);

                float H = hsv[0]; // [0,1]
                float S = hsv[1]; // [0,1]

                int binH = (int) (H * binsH);
                int binS = (int) (S * binsS);
                if (binH >= binsH) binH = binsH - 1;
                if (binS >= binsS) binS = binsS - 1;

                int idx = binH * binsS + binS;
                hist[idx] += 1.0f;
            }
        }

        double sum = 0.0;
        for (float v : hist) sum += v;
        if (sum > 0) {
            for (int i = 0; i < size; i++) {
                hist[i] /= (float) sum;
            }
        }
        return hist;
    }

        public float[] hist3D(byte[] data, int binsR, int binsG, int binsB) {
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                throw new IllegalArgumentException("Impossible de décoder l’image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur de décodage image", e);
        }

        int w = img.getWidth();
        int h = img.getHeight();

        int size = binsR * binsG * binsB;
        float[] hist = new float[size];

        float stepR = 256f / binsR;
        float stepG = 256f / binsG;
        float stepB = 256f / binsB;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                int binR = (int) (r / stepR);
                int binG = (int) (g / stepG);
                int binB = (int) (b / stepB);

                if (binR >= binsR) binR = binsR - 1;
                if (binG >= binsG) binG = binsG - 1;
                if (binB >= binsB) binB = binsB - 1;

                int idx = binR * (binsG * binsB) + binG * binsB + binB;
                hist[idx] += 1.0f;
            }
        }

        double sum = 0.0;
        for (float v : hist) sum += v;
        if (sum > 0) {
            for (int i = 0; i < size; i++) {
                hist[i] /= (float) sum;
            }
        }
        return hist;
    }

}
