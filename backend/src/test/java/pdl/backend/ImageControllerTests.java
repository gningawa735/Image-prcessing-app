package pdl.backend;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class ImageControllerTests {

	@MockitoBean
	private ImageDao imageDao;
	@MockitoBean
	private Image image;

	@Autowired
	private MockMvc mockMvc;

	
  @Test
  public void getSimilarImagesShouldReturnSuccessForH1D() throws Exception {
    Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
    image.setId(1L);

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
    when(imageDao.similarImages(1L, 5, 1))
        .thenReturn(List.of(Map.of("id", 2L, "score", 0.42)));

    mockMvc.perform(get("/images/1/similar")
            .param("number", "5")
            .param("descriptor", "H1D"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json; charset=UTF-8"))
        .andExpect(jsonPath("$[0].id").value(2))
        .andExpect(jsonPath("$[0].score").value(0.42));

    verify(imageDao).retrieve(1L);
    verify(imageDao).similarImages(1L, 5, 1);
  }
  @Test
    public void getSimilarImagesShouldReturnNotFound() throws Exception {
    when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/images/999/similar")
            .param("number", "5")
            .param("descriptor", "H1D"))
        .andExpect(status().isNotFound());

    verify(imageDao).retrieve(999L);
    }

    @Test
    public void getSimilarImagesShouldReturnBadRequest() throws Exception {
        Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
        image.setId(1L);

        when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

        mockMvc.perform(get("/images/1/similar")
                .param("number", "5")
                .param("descriptor", "XYZ"))
            .andExpect(status().isBadRequest());

        verify(imageDao).retrieve(1L);
  }
  @Test
  public void getImageMetadataShouldReturnSuccess() throws Exception {
    Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
    image.setId(1L);
    image.setWidth(640);
    image.setHeight(480);
    image.addKeyword("plat");
    image.addKeyword("afrique");

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

    mockMvc.perform(get("/images/1/metadata"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json; charset=UTF-8"))
        .andExpect(jsonPath("$.name").value("ndole.jpg"))
        .andExpect(jsonPath("$.type").value("image/jpeg"))
        .andExpect(jsonPath("$.size").value("640x480"))
        .andExpect(jsonPath("$.keywords").isArray());

    verify(imageDao).retrieve(1L);
  }

  @Test
  public void getImageMetadataShouldReturnNotFound() throws Exception {
    when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/images/999/metadata"))
        .andExpect(status().isNotFound());

    verify(imageDao).retrieve(999L);
  }

  @Test
  public void addKeywordShouldReturnNoContent() throws Exception {
    Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
    image.setId(1L);

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
    doNothing().when(imageDao).addKeyword(1L, "cuisine");

    mockMvc.perform(put("/images/1/keywords")
            .param("tag", "cuisine"))
        .andExpect(status().isNoContent());

    verify(imageDao).retrieve(1L);
    verify(imageDao).addKeyword(1L, "cuisine");
  }

  @Test
  public void addKeywordShouldReturnNotFound() throws Exception {
    when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

    mockMvc.perform(put("/images/999/keywords")
            .param("tag", "cuisine"))
        .andExpect(status().isNotFound());

    verify(imageDao).retrieve(999L);
  }

  @Test
  public void deleteKeywordShouldReturnNoContent() throws Exception {
    Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
    image.setId(1L);

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
    when(imageDao.hasKeyword(1L, "cuisine")).thenReturn(true);
    doNothing().when(imageDao).deleteKeyword(1L, "cuisine");

    mockMvc.perform(delete("/images/1/keywords")
            .param("tag", "cuisine"))
        .andExpect(status().isNoContent());

    verify(imageDao).retrieve(1L);
    verify(imageDao).hasKeyword(1L, "cuisine");
    verify(imageDao).deleteKeyword(1L, "cuisine");
  }

  @Test
  public void deleteKeywordShouldReturnBadRequest() throws Exception {
    Image image = new Image("ndole.jpg", new byte[] {1, 2, 3});
    image.setId(1L);

    when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
    when(imageDao.hasKeyword(1L, "inexistant")).thenReturn(false);

    mockMvc.perform(delete("/images/1/keywords")
            .param("tag", "inexistant"))
        .andExpect(status().isBadRequest());

    verify(imageDao).retrieve(1L);
    verify(imageDao).hasKeyword(1L, "inexistant");
  }

  @Test
  public void deleteKeywordShouldReturnNotFound() throws Exception {
    when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

    mockMvc.perform(delete("/images/999/keywords")
            .param("tag", "cuisine"))
        .andExpect(status().isNotFound());

    verify(imageDao).retrieve(999L);
  }
}