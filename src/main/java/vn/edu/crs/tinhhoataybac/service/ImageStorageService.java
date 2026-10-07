package vn.edu.crs.tinhhoataybac.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
  public String save(MultipartFile file) {
    if (file == null || file.isEmpty()) return null;
    if (file.getSize() > 5 * 1024 * 1024) throw new IllegalStateException("Ảnh tối đa 5 MB.");
    try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new IllegalStateException("Chọn ảnh PNG hoặc JPEG hợp lệ.");
      var reader = readers.next();
      try {
        reader.setInput(stream);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w <= 0 || h <= 0 || (long) w * h > 20000000)
          throw new IllegalStateException("Ảnh có kích thước quá lớn.");
        var image = reader.read(0);
        Path dir = Path.of("uploads").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + ".png";
        Path target = dir.resolve(name);
        if (!ImageIO.write(image, "png", target.toFile()))
          throw new IllegalStateException("Không thể lưu ảnh.");
        return "/uploads/" + name;
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Không thể đọc hoặc lưu ảnh.", e);
    }
  }
}
