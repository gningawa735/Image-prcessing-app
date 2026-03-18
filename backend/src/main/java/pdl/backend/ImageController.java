package pdl.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ObjectMapper mapper;

    private final ImageDao imageDao;

    public ImageController(ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    // Besoin 5 : liste d’ids
    @RequestMapping(value = "/images", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ArrayNode getImageList() {
        ArrayNode nodes = mapper.createArrayNode();
        for (Image img : imageDao.retrieveAll()) {
            nodes.add(img.getId());
        }
        return nodes;
    }

    // Besoin 7 : upload image JPEG/PNG
    @RequestMapping(value = "/images", method = RequestMethod.POST)
    public ResponseEntity<?> addImage(@RequestParam MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String contentType = file.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try {
            Image img = new Image(file.getOriginalFilename(), file.getBytes());
            imageDao.create(img);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Besoin 6 : récupération d’une image
    @RequestMapping(value = "/images/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getImage(@PathVariable long id) {
        Optional<Image> opt = imageDao.retrieve(id);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Image image = opt.get();
        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image.getData());
    }

    // Besoin 8 : métadonnées
    @RequestMapping(
            value = "/images/{id}/metadata",
            method = RequestMethod.GET,
            produces = "application/json; charset=UTF-8"
    )
    public ResponseEntity<?> getImageMetadata(@PathVariable long id) {
        Optional<Image> opt = imageDao.retrieve(id);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Image img = opt.get();

        ObjectNode node = mapper.createObjectNode();
        node.put("name", img.getName());
        node.put("type", "image/jpeg");
        node.put("size", img.getWidth() + "x" + img.getHeight());

        ArrayNode keywordsNode = mapper.createArrayNode();
        for (String k : img.getKeywords()) {
            keywordsNode.add(k);
        }
        node.set("keywords", keywordsNode);

        return new ResponseEntity<>(node, HttpStatus.OK);
    }

    // Besoin 9 : suppression
    @RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteImage(@PathVariable long id) {
        Optional<Image> opt = imageDao.retrieve(id);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        imageDao.delete(opt.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Besoin 14 : similaires
    @RequestMapping(value = "/images/{id}/similar", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> getSimilarImages(
            @PathVariable long id,
            @RequestParam int number,
            @RequestParam String descriptor) {

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

    // Ajouter un mot-clé (PUT /images/{id}/keywords?tag=...)
    @RequestMapping(value = "/images/{id}/keywords", method = RequestMethod.PUT)
    public ResponseEntity<?> addKeyword(
            @PathVariable long id,
            @RequestParam("tag") String tag) {

        Optional<Image> opt = imageDao.retrieve(id);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        imageDao.addKeyword(id, tag);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Supprimer un mot-clé (DELETE /images/{id}/keywords?tag=...)
    @RequestMapping(value = "/images/{id}/keywords", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteKeyword(
            @PathVariable long id,
            @RequestParam("tag") String tag) {

        Optional<Image> opt = imageDao.retrieve(id);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean has = imageDao.hasKeyword(id, tag);
        if (!has) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        imageDao.deleteKeyword(id, tag);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Besoin 15 : mots-clés disponibles
    @RequestMapping(value = "/images/keywords", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ArrayNode getKeywords() {
        ArrayNode nodes = mapper.createArrayNode();
        for (String keyword : imageDao.getAllKeywords()) {
            nodes.add(keyword);
        }
        return nodes;
    }

    // Besoin 23 (client) + 15 (serveur) : recherche par attributs
    @RequestMapping(value = "/images/search", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> searchImages(@RequestBody ImageSearchAttributes attrs) {
        String name = attrs.getName();
        String format = attrs.getFormat();
        Integer dimension = attrs.getDimension();
        String keyword = attrs.getKeyword();

        if ((name == null || name.isBlank())
                && (format == null || format.isBlank())
                && dimension == null
                && (keyword == null || keyword.isBlank())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Image> results = null;

        if (name != null && !name.isBlank()) {
            results = imageDao.findByName(name);
        }

        if (format != null && !format.isBlank()) {
            List<Image> imagesByFormat = imageDao.findByFormat(format);
            results = intersect(results, imagesByFormat);
        }

        if (dimension != null) {
            List<Image> imagesByDimension = imageDao.findByDimension(dimension);
            results = intersect(results, imagesByDimension);
        }

        if (keyword != null && !keyword.isBlank()) {
            List<Image> imagesByKeyword = imageDao.findByKeyword(keyword);
            results = intersect(results, imagesByKeyword);
        }

        if (results == null || results.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ArrayNode nodes = mapper.createArrayNode();
        for (Image image : results) {
            nodes.add(image.getId());
        }

        return new ResponseEntity<>(nodes, HttpStatus.OK);
    }

    private List<Image> intersect(List<Image> base, List<Image> other) {
        if (base == null) {
            return other;
        }
        if (other == null) {
            return base;
        }
        return base.stream()
                .filter(image1 -> other.stream()
                        .anyMatch(image2 -> image1.getId().equals(image2.getId())))
                .toList();
    }
}
