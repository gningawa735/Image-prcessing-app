package pdl.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ImageDao implements Dao<Image>, InitializingBean {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final RowMapper<Image> imageRowMapper = new RowMapper<Image>() {
    @Override
    public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
      String name = rs.getString("name");
      String path = rs.getString("path");
      long id = rs.getLong("id");

      try {
        byte[] data = Files.readAllBytes(new File(path).toPath());
        return new Image(name, data, id);
      } catch (IOException e) {
        throw new RuntimeException("Impossible de lire le fichier : " + path, e);
      }
    }
  };

  @Override
  public void afterPropertiesSet() {
    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

    jdbcTemplate.execute("""
      CREATE TABLE IF NOT EXISTS images (
        id BIGINT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        path VARCHAR(255) NOT NULL,
        size BIGINT NOT NULL,
        type VARCHAR(100) NOT NULL,
        descriptor1d vector(9),
        descriptor2d vector(32),
        descriptor3d vector(64)
      )
      """);
  }

  @Override
  public void create(Image image) {
    try {
      String filePath = "images/" + image.getId() + "_" + image.getName();
      try (FileOutputStream fos = new FileOutputStream(filePath)) {
        fos.write(image.getData());
      }
      String d1 = null;
      String d2 = null;
      String d3 = null;
      if (image.getDescriptor() != null) {
        String vector = java.util.Arrays.toString(image.getDescriptor()).replace(" ", "");
        if (image.getDescriptorType() == 1) {
          d1 = vector;
        } else if (image.getDescriptorType() == 2) {
          d2 = vector;
        } else if (image.getDescriptorType() == 3) {
          d3 = vector;
        }
      }
      jdbcTemplate.update(
        "INSERT INTO images (id, name, path, size, type, descriptor1d, descriptor2d, descriptor3d) VALUES (?, ?, ?, ?, ?, ?::vector, ?::vector, ?::vector)",
        image.getId(),
        image.getName(),
        filePath,
        image.getData().length,
        image.getName().lastIndexOf('.') > 0 ? image.getName().substring(image.getName().lastIndexOf('.') + 1) : "unknown",
        d1,
        d2,
        d3
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Image> retrieve(long id) {
    List<Image> result = jdbcTemplate.query(
      "SELECT * FROM images WHERE id = ?",
      imageRowMapper,
      id
    );
    return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
  }

  @Override
  public List<Image> retrieveAll() {
    return jdbcTemplate.query("SELECT * FROM images", imageRowMapper);
  }

  @Override
  public void update(Image image, String[] params) {
  }

  @Override
  public void delete(Image image) {
    jdbcTemplate.update("DELETE FROM images WHERE id = ?", image.getId());
  }

  public List<Image> similarImages(long id, int limit, int descriptorType) {
    String column =
      descriptorType == 1 ? "descriptor1d" :
      descriptorType == 2 ? "descriptor2d" :
      "descriptor3d";

    return jdbcTemplate.query(
      "SELECT * FROM images " +
      "WHERE id <> ? " +
      "AND " + column + " IS NOT NULL " +
      "ORDER BY " + column + " <-> (SELECT " + column + " FROM images WHERE id = ?) " +
      "LIMIT ?",
      imageRowMapper,
      id, id, limit
    );
  }

    public List<Image> findByName(String name) {
      return jdbcTemplate.query(  "SELECT * FROM images WHERE name LIKE ?",  imageRowMapper,   "%" + name + "%" );
    }

    public List<Image> findByFormat(String format) {
     return jdbcTemplate.query( "SELECT * FROM images WHERE type = ?",  imageRowMapper, format);
    }

  public List<Image> findByKeyword(String keyword) {
    return retrieveAll()
        .stream()
        .filter(img -> img.getKeywords().contains(keyword))
        .toList();
  }

}