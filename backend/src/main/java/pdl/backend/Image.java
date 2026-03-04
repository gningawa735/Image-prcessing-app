package pdl.backend;

import java.util.HashSet;
import java.util.Set;

public class Image {
  private static Long count = Long.valueOf(0);
  private Long id;
  private String name;
  private byte[] data;
  private Set<String> keywords;

  public Image(final String name, final byte[] data) {
    id = count++;
    this.name = name;
    this.data = data;
    this.keywords = new HashSet<>();
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public byte[] getData() {
    return data;
  }
  
  public Set<String> getKeywords() { return keywords; }

  public void addKeyword(String tag) {
    this.keywords.add(tag);
  }
}
