package pdl.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTests {

  @Mock
  private ImageDao imageDao;

  @Mock
  private DescriptorService descriptorService;

  @InjectMocks
  private ImageService imageService;

  private Path createdTestFile;

  @AfterEach
  void cleanup() throws IOException {
    if (createdTestFile != null) {
      Files.deleteIfExists(createdTestFile);
    }
  }

  @Test
  public void getImagesListShouldReturnDaoImages() {
    Image img1 = new Image("img1.jpg", new byte[] {1});
    Image img2 = new Image("img2.jpg", new byte[] {2});

    when(imageDao.retrieveAll()).thenReturn(List.of(img1, img2));

    List<Image> result = imageService.getImagesList();

    assertTrue(result.size() == 2);
    verify(imageDao).retrieveAll();
  }

  @Test
  public void addKeywordShouldAddKeywordToRetrievedImage() {
    Image image = new Image("img.jpg", new byte[] {1});
    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

    imageService.addKeyword(1L, "cuisine");

    assertTrue(image.getKeywords().contains("cuisine"));
    verify(imageDao).retrieve(1L);
  }

  @Test
  public void deleteImageShouldReturnFalseWhenImageDoesNotExist() {
    when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

    boolean deleted = imageService.deleteImage(1L);

    assertFalse(deleted);
    verify(imageDao).retrieve(1L);
    verify(imageDao, never()).delete(any());
  }

  @Test
  public void deleteImageShouldDeleteFileAndReturnTrue() throws Exception {
    Path imagesDir = Paths.get("images");
    Files.createDirectories(imagesDir);

    String uniqueName = "test-delete-" + UUID.randomUUID() + ".jpg";
    Path filePath = imagesDir.resolve(uniqueName);
    Files.write(filePath, new byte[] {1, 2, 3});
    createdTestFile = filePath;

    Image image = new Image(uniqueName, new byte[] {1, 2, 3});
    image.setId(1L);

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

    boolean deleted = imageService.deleteImage(1L);

    assertTrue(deleted);
    assertFalse(Files.exists(filePath));
    verify(imageDao).delete(image);

    createdTestFile = null;
  }

  @Test
  public void initImagesFromDiskShouldCreateImageWhenNotIndexed() throws Exception {
    Path imagesDir = Paths.get("images");
    Files.createDirectories(imagesDir);

    String uniqueName = "test-init-" + UUID.randomUUID() + ".png";
    createdTestFile = imagesDir.resolve(uniqueName);

    BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(bufferedImage, "png", createdTestFile.toFile());

    when(imageDao.existsByName(any())).thenReturn(true);
    when(imageDao.existsByName(uniqueName)).thenReturn(false);
    when(imageDao.retrieveAll()).thenReturn(List.of());

    when(descriptorService.compute(any(BufferedImage.class), eq("H1D")))
        .thenReturn(new float[] {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f});
    when(descriptorService.compute(any(BufferedImage.class), eq("H2D")))
        .thenReturn(new float[64]);
    when(descriptorService.compute(any(BufferedImage.class), eq("H3D")))
        .thenReturn(new float[64]);

    imageService.initImagesFromDisk();

    ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
    verify(imageDao).create(captor.capture());

    Image saved = captor.getValue();
    assertTrue(saved.getName().equals(uniqueName));
    assertTrue(saved.getHist1D() != null);
    assertTrue(saved.getHist2D() != null);
    assertTrue(saved.getHist3D() != null);
  }

  @Test
  public void initImagesFromDiskShouldSkipImageWhenAlreadyIndexed() throws Exception {
    Path imagesDir = Paths.get("images");
    Files.createDirectories(imagesDir);

    String uniqueName = "test-skip-" + UUID.randomUUID() + ".png";
    createdTestFile = imagesDir.resolve(uniqueName);

    BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(bufferedImage, "png", createdTestFile.toFile());

    when(imageDao.existsByName(any())).thenReturn(true);
    when(imageDao.retrieveAll()).thenReturn(List.of());

    imageService.initImagesFromDisk();

    verify(imageDao, never()).create(argThat(img -> uniqueName.equals(img.getName())));
  }
}