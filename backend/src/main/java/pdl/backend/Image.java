package pdl.backend;

import java.util.HashSet;
import java.util.Set;

public class Image {
  private Long id;
  private String name;
  private byte[] data;
  private int height;
  private int width;
  private Set<String> keywords;
  private float[] hist1D;
  private float[] hist2D;
  private float[] hist3D;

   public Image(final String name, final byte[] data) {
    this.name = name;
    this.data = data;
    this.keywords = new HashSet<>();
  }


  public Long getId() {
  return id;
  }

  public void setId(Long id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }
  
  public int getWidth(){
    return width;
  }

  public void setWidth(int width){
      this.width=width;
  }

  public int getHeight(){
    return height;
  }

  public void setHeight(int height){
      this.height=height;
  }
  public byte[] getData() {
    return data;
  }
  
  public Set<String> getKeywords() { return keywords; }

  public void addKeyword(String tag) {
    this.keywords.add(tag);
  }

  public void setHist1D(float[] hist) { this.hist1D = hist; }
  public void setHist2D(float[] hist) { this.hist2D = hist; }
  public void setHist3D(float[] hist) { this.hist3D = hist; }

  public float[] getHist1D() { return hist1D; }
  public float[] getHist2D() { return hist2D; }
  public float[] getHist3D() { return hist3D; }
}
