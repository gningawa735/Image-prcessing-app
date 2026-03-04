package pdl.backend;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ImageDao implements Dao<Image> {


    private final Map<Long, Image> images = new HashMap<>();

    @Override
    public void create(Image image) {
        images.put(image.getId(), image);
    }

    @Override
    public Optional<Image> retrieve(long id) {
        return Optional.ofNullable(images.get(id));
    }

    @Override
    public List<Image> retrieveAll() {
        return new ArrayList<>(images.values());
    }

    @Override
    public void update(Image image, String[] params) {

    }

    @Override
    public void delete(Image image) {
        images.remove(image.getId());
    }

}