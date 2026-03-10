package pdl.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

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
        throw new IllegalStateException(
                "Besoin 1: dossier 'images' introuvable. Attendu: " + imagesDir
                        + " (working dir: " + Paths.get("").toAbsolutePath() + ")"
        );
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(imagesDir)) { 
        for (Path p : stream) {  
            if (Files.isDirectory(p)) continue;
            if (!isSupportedImage(p)) continue;

            try {
                byte[] data = Files.readAllBytes(p);
                String name = p.getFileName().toString();
                
                Image img = new Image(name, data);

                int type = 1;
                img.setDescriptorType(type);
                int bins = 9;
                img.setDescriptor(descriptorService.hist1D(data, bins));

                imageDao.create(img);
                System.out.println("[BOOT] + " + name + " (HOG1D[" + bins + "]) (" + data.length + " bytes)");
            } catch (Exception e) {
                System.err.println("[BOOT] SKIP " + p.getFileName() + " : " + e.getMessage());
            }
        }
        System.out.println("[BOOT] Images chargées: " + imageDao.retrieveAll().size());
    } catch (IOException e) {
        throw new IllegalStateException("Erreur lecture dossier images: " + imagesDir, e);
    }
    }


    private boolean isSupportedImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
    }

    public void saveImage(String fileName, byte[] data) throws IOException {
        Path path = Paths.get("images").resolve(fileName);
        Files.write(path, data);

        Image newImage = new Image(fileName, data);
        
        int type = 1; 
        newImage.setDescriptorType(type);
        int bins = 9;
        newImage.setDescriptor(descriptorService.hist1D(data, bins));

        imageDao.create(newImage);
        System.out.println("[SAVE] + " + fileName + " (HOG1D[" + bins + "])");
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
