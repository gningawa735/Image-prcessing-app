package pdl.backend;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class ImageControllerTests {

	@MockitoBean
	private ImageDao imageDAO;
	@MockitoBean
	private Image image;

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void getImageShouldReturnSuccess() throws Exception {
		when(imageDAO.retrieve(0)).thenReturn(Optional.ofNullable(image));
		this.mockMvc.perform(get("/images/0")).andExpect(status().isOk());
		verify(imageDAO).retrieve(0);
	}

	@Test
	public void getImageShouldReturnNotFound() throws Exception {
		when(imageDAO.retrieve(0)).thenReturn(Optional.empty());

		this.mockMvc.perform(get("/images/0"))
				.andExpect(status().isNotFound());

		verify(imageDAO).retrieve(0);
	}

	@Test
	public void getImageListShouldReturnJson() throws Exception {
		Image img1 = new Image("img1.jpg", new byte[] { 1, 2, 3 });
		Image img2 = new Image("img2.jpg", new byte[] { 4, 5, 6 });
		when(imageDAO.retrieveAll()).thenReturn(Arrays.asList(img1, img2));

		mockMvc.perform(get("/images"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[0].id").value(img1.getId()))
				.andExpect(jsonPath("$[0].name").value("img1.jpg"))
				.andExpect(jsonPath("$[1].id").value(img2.getId()))
				.andExpect(jsonPath("$[1].name").value("img2.jpg"));
	}

	@Test
	public void addImageShouldReturnSuccess() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"image.jpg",
				MediaType.IMAGE_JPEG_VALUE,
				new ClassPathResource("test.jpg").getInputStream());

		this.mockMvc.perform(
				multipart("/images").file(file)).andExpect(status().isCreated());

		verify(imageDAO).create(org.mockito.ArgumentMatchers.any(Image.class));
	}

	@Test
	public void addImageShouldReturnUnsupportedMediaType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"file.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"not an image".getBytes());

		this.mockMvc.perform(
				multipart("/images").file(file)).andExpect(status().isUnsupportedMediaType());
	}

	@Test
	public void deleteImagesShouldReturnMethodNotAllowed() throws Exception {
		this.mockMvc.perform(delete("/images"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	public void deleteImageShouldReturnNotFound() throws Exception {
		when(imageDAO.retrieve(0)).thenReturn(Optional.empty());

		this.mockMvc.perform(delete("/images/0"))
				.andExpect(status().isNotFound());

		verify(imageDAO).retrieve(0);
	}

	@Test
	public void deleteImageShouldReturnSuccess() throws Exception {
		when(imageDAO.retrieve(0)).thenReturn(Optional.of(image));

		this.mockMvc.perform(delete("/images/0"))
				.andExpect(status().isNoContent());

		verify(imageDAO).retrieve(0);
		verify(imageDAO).delete(image);
	}

	@Test
	public void getImageListShouldReturnSuccess() throws Exception {
		when(imageDAO.retrieveAll()).thenReturn(new ArrayList<>());

		this.mockMvc.perform(get("/images"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));

		verify(imageDAO).retrieveAll();
	}

	@Test
	public void deleteImageShouldReturnNoContent() throws Exception {
		Image img = new Image("img.jpg", new byte[] { 1 });
		when(imageDAO.retrieve(0L)).thenReturn(Optional.of(img));

		mockMvc.perform(delete("/images/0"))
				.andExpect(status().isNoContent());

		verify(imageDAO).delete(img);
	}

}