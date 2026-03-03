package pdl.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

@Service
public class ImageService {

    private final ImageDao imageDao;

    public ImageService(ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    @PostConstruct
    public void initImagesFromDisk() {
        Path imagesDir = Paths.get("images").toAbsolutePath().normalize();

        // Besoin 1 : erreur explicite si dossier absent
        if (!Files.exists(imagesDir) || !Files.isDirectory(imagesDir)) {
            throw new IllegalStateException(
                    "Besoin 1: dossier 'images' introuvable. Attendu: " + imagesDir
                            + " (working dir: " + Paths.get("").toAbsolutePath() + ")"
            );
        }

        // Besoin 1 : ignorer sous-dossiers + filtrer extensions
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imagesDir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) continue;      // pas de sous-dossiers
                if (!isSupportedImage(p)) continue;      // pas d'autres formats

                byte[] data = Files.readAllBytes(p);
                String name = p.getFileName().toString();

                // Pour TP1: on "charge" l'image en mémoire (DAO)
                imageDao.create(new Image(name, data));
                System.out.println("[BOOT] + " + name + " (" + data.length + " bytes)");
            }
             System.out.println("[BOOT] Images chargées: " 
                + imageDao.retrieveAll().size());
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lecture dossier images: " + imagesDir, e);
        }
    }

    private boolean isSupportedImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
    }
}