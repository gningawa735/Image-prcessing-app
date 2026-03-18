package pdl.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@CrossOrigin(origins = "*")
public class ImageController {

  @Autowired
  private ObjectMapper mapper;

  private final ImageDao imageDao;

  public ImageController(ImageDao imageDao) {
    this.imageDao = imageDao;
  }


  @RequestMapping(value = "/images", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ArrayNode getImageList() {
    ArrayNode nodes = mapper.createArrayNode();

    List<Image> images = imageDao.retrieveAll();
    for (Image image : images) {
      ObjectNode node = mapper.createObjectNode();
      node.put("name", image.getName());
      node.put("id", image.getId());
      nodes.add(node);
    }
    return nodes;
  }

  @RequestMapping(value = "/images", method = RequestMethod.POST)
  public ResponseEntity<?> addImage(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
    if (file.isEmpty())
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    String contentType = file.getContentType();
    if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) &&     !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
      return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    try {
      Image image = new Image(file.getOriginalFilename(), file.getBytes());
      imageDao.create(image);
      return new ResponseEntity<>(HttpStatus.CREATED);
    } catch (IOException e) {
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(value = "/images/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<?> getImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    Image image = imageOpt.get();
    MediaType mediaType;
    String lowerName = image.getName().toLowerCase();
    if (lowerName.endsWith(".png")) {
      mediaType = MediaType.IMAGE_PNG;
    } else {
      mediaType = MediaType.IMAGE_JPEG;
    }
    return ResponseEntity
      .ok()
      .contentType(mediaType)
      .body(image.getData());
  }
  
  @RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    Image image = imageOpt.get();
    imageDao.delete(image);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @RequestMapping(value = "/images/{id}/similar", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<?> getSimilarImages(@PathVariable long id, @RequestParam int number, @RequestParam String descriptor) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    int descriptorType;
    switch (descriptor) {
      case "H1D":
        descriptorType = 1;
        break;
      case "H2D":
        descriptorType = 2;
        break;
      case "H3D":
        descriptorType = 3;
        break;
      default:
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    List<Map<String, Object>> images = imageDao.similarImages(id, number, descriptorType);  
    ArrayNode nodes = mapper.createArrayNode();
    for (Map<String, Object> image : images) {
      ObjectNode node = mapper.createObjectNode();
      node.put("id", ((Number) image.get("id")).longValue());
      node.put("score", ((Number) image.get("score")).doubleValue());
      nodes.add(node);
    }

    return new ResponseEntity<>(nodes, HttpStatus.OK);
  }


  @RequestMapping(value = "/images/{id}/metadata", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<?> getImageMetadata(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    Image image = imageOpt.get();

    String lowerName = image.getName().toLowerCase();
    String type;

    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
      type = MediaType.IMAGE_JPEG_VALUE;
     } else if (lowerName.endsWith(".png")) {
      type = MediaType.IMAGE_PNG_VALUE;
    } else {
      type = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    ObjectNode node = mapper.createObjectNode();
    node.put("name", image.getName());
    node.put("type", type);
    node.put("size", image.getWidth() + "x" + image.getHeight());

    ArrayNode keywordsNode = mapper.createArrayNode();
    for (String keyword : image.getKeywords()) {
      keywordsNode.add(keyword);
    }
    node.set("keywords", keywordsNode);

    return new ResponseEntity<>(node, HttpStatus.OK);
  }
  @RequestMapping(value = "/images/{id}/keywords", method = RequestMethod.PUT)
  @ResponseBody
  public ResponseEntity<?> addKeyword(@PathVariable long id, @RequestParam String tag) {
      Optional<Image> imageOpt = imageDao.retrieve(id);
      if (imageOpt.isEmpty()) {
          return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      imageDao.addKeyword(id, tag);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @RequestMapping(value = "/images/{id}/keywords", method = RequestMethod.DELETE)
  @ResponseBody
  public ResponseEntity<?> deleteKeyword(@PathVariable long id, @RequestParam String tag) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if (!imageDao.hasKeyword(id, tag)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    imageDao.deleteKeyword(id, tag);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
 @RequestMapping(value = "/images/{id}/info", method = RequestMethod.GET, produces = "application/json")
@ResponseBody
public ResponseEntity<?> getImageInfo(@PathVariable long id) {
    return imageDao.retrieve(id)
        .map(img -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", img.getId());
            node.put("name", img.getName());
            node.put("size", img.getData().length); 
            return ResponseEntity.ok((com.fasterxml.jackson.databind.JsonNode) node);
        })
        .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  
    }
}