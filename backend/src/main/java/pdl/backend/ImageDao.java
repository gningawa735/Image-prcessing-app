package pdl.backend;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.awt.image.BufferedImage;

@Repository
public class ImageDao implements Dao<Image>, InitializingBean {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final RowMapper<Image> imageRowMapper = new RowMapper<Image>() {
    @Override
    public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
      long id = rs.getLong("id");
      String name = rs.getString("name");
      String path = rs.getString("path");
      int width = rs.getInt("width");
      int height = rs.getInt("height");

      try {
        byte[] data = Files.readAllBytes(new File(path).toPath());
        Image img = new Image(name, data);
        img.setId(id);
        img.setWidth(width);
        img.setHeight(height);

        List<String> keywords = jdbcTemplate.queryForList(
                "SELECT keyword FROM image_keywords WHERE image_id = ?",
                String.class, id
        );
        for (String kw : keywords) img.addKeyword(kw);
        return img;
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
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        path VARCHAR(255) NOT NULL,
        size BIGINT NOT NULL,
        type VARCHAR(100) NOT NULL,
        width INT,
        height INT,
        descriptor1d vector(9),
        descriptor2d vector(64),
        descriptor3d vector(64)
      )
      """);

    jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS image_keywords (
          image_id BIGINT NOT NULL REFERENCES images(id) ON DELETE CASCADE,
          keyword VARCHAR(255) NOT NULL,
          PRIMARY KEY (image_id, keyword)
        )
        """);
  }

  @Override
  public void create(Image image) {
    try {
      String filePath = "images/" + image.getName();
      try (FileOutputStream fos = new FileOutputStream(filePath)) {
        fos.write(image.getData());
      }
      BufferedImage img = ImageIO.read(new ByteArrayInputStream(image.getData()));
      int width = img.getWidth();
      int height = img.getHeight();

      image.setWidth(width);
      image.setHeight(height);

      String d1 = null;
      String d2 = null;
      String d3 = null;
      if (image.getHist1D() != null){
        d1 = java.util.Arrays.toString(image.getHist1D()).replace(" ", "");

        if (image.getHist2D() != null)
        d2 = java.util.Arrays.toString(image.getHist2D()).replace(" ", "");

        if (image.getHist3D() != null)
        d3 = java.util.Arrays.toString(image.getHist3D()).replace(" ", "");
    }
      jdbcTemplate.update("INSERT INTO images (name, path, size, type, width, height, descriptor1d, descriptor2d, descriptor3d) VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?::vector, ?::vector)",
        image.getName(),
        filePath.toString(),
        image.getData().length,
        image.getName().lastIndexOf('.') > 0 ? image.getName().substring(image.getName().lastIndexOf('.') + 1) : "unknown",
        width,
        height,
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

  public List<Map<String, Object>> similarImages(long id, int limit, int descriptorType) {
    String column =
      descriptorType == 1 ? "descriptor1d" :
      descriptorType == 2 ? "descriptor2d" :
      "descriptor3d";

    return jdbcTemplate.queryForList(
      "SELECT id, " +
      column + " <-> (SELECT " + column + " FROM images WHERE id = ?) AS score " +
      "FROM images " +
      "WHERE id <> ? " +
      "AND " + column + " IS NOT NULL " +
      "ORDER BY score " +
      "LIMIT ?",
      id, id, limit
    );
  }
  public void addKeyword(long id, String keyword) {
    jdbcTemplate.update(
        "INSERT INTO image_keywords (image_id, keyword) VALUES (?, ?) ON CONFLICT DO NOTHING",
        id, keyword
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

  public List<Image> findByDimension(int value) {
    return jdbcTemplate.query( "SELECT * FROM images WHERE width = ? OR height = ?",  imageRowMapper,value, value);
  }
  public boolean existsByName(String name) {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM images WHERE name = ?", Integer.class,name);
    return count != null && count > 0;
  }


}