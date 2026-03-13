package pdl.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ImageService {

    private final ImageDao imageDao;
    private final DescriptorService descriptorService;

    public ImageService(ImageDao imageDao, DescriptorService descriptorService) {
        this.imageDao = imageDao;
        this.descriptorService = descriptorService;
    }

@PostConstruct
public void initImagesFromDisk() {
    Path imagesDir = Paths.get("images").toAbsolutePath().normalize();

    if (!Files.exists(imagesDir) || !Files.isDirectory(imagesDir)) {
        throw new IllegalStateException("Besoin 1: dossier 'images' introuvable.");
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(imagesDir)) { 
        for (Path p : stream) {  
            if (Files.isDirectory(p) || !isSupportedImage(p)) continue;

            try {
                byte[] data = Files.readAllBytes(p);
                String name = p.getFileName().toString();
                if (imageDao.existsByName(name)) {
                    System.out.println("[BOOT] SKIP déjà indexée : " + name);
                    continue;
                }
                indexAndSaveImage(name, data);
                System.out.println("[BOOT] + " + name + " indexée (H1D, H2D, H3D)");
            } catch (Exception e) {
                System.err.println("[BOOT] SKIP " + p.getFileName() + " : " + e.getMessage());
            }
        }
        System.out.println("[BOOT] Images chargées: " + imageDao.retrieveAll().size());
    } catch (IOException e) {
        throw new IllegalStateException("Erreur lecture dossier images", e);
    }
}

private void indexAndSaveImage(String fileName, byte[] data) throws IOException {
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
    if (img == null) throw new IOException("Impossible de décoder l'image : " + fileName);

    Image image = new Image(fileName, data);
    image.setHist1D(descriptorService.compute(img, "H1D"));
    image.setHist2D(descriptorService.compute(img, "H2D"));
    image.setHist3D(descriptorService.compute(img, "H3D"));

    imageDao.create(image);
}

private boolean isSupportedImage(Path p) {
    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
    return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
}

    public boolean deleteImage(long id) {
        Optional<Image> imageOpt = imageDao.retrieve(id);
        if (imageOpt.isPresent()) {
            Image image = imageOpt.get();
            try {
                Path path = Paths.get("images").resolve(image.getName());
                Files.deleteIfExists(path);
                
                imageDao.delete(image);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public void addKeyword(long id, String tag) {
        imageDao.retrieve(id).ifPresent(img -> img.addKeyword(tag));
    }

    public List<Image> getImagesList() {
        return imageDao.retrieveAll();
    }
}
