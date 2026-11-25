package vn.edu.tlu.dms.maintenance.service.impl;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.PhotoUpload;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.maintenance.domain.MaintenancePhoto;
import vn.edu.tlu.dms.maintenance.repository.MaintenancePhotoRepository;
import vn.edu.tlu.dms.maintenance.service.MaintenancePhotoService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MaintenancePhotoServiceImpl implements MaintenancePhotoService {

    private final Path uploadDirectory;

    private final MaintenancePhotoRepository repository;

    public MaintenancePhotoServiceImpl(
            @Value("${dms.upload-dir}") String directory, MaintenancePhotoRepository repository) {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.repository = repository;
    }

    /**
     * Tên file do server sinh. Kiểm tra nội dung thật, không tin extension hoặc Content-Type của
     * client.
     */
    @Override()
    public void savePhotos(Long requestId, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        if (photos.size() > PhotoUpload.MAX_COUNT) {
            throw new BusinessException(
                    ErrorCode.PHOTO_INVALID, HttpStatus.BAD_REQUEST, Map.of(ApiField.REASON, PhotoUpload.REASON_COUNT));
        }
        for (MultipartFile photo : photos) {
            if (photo.isEmpty() || photo.getSize() > PhotoUpload.MAX_BYTES) {
                throw new BusinessException(
                        ErrorCode.PHOTO_INVALID, HttpStatus.BAD_REQUEST, Map.of(ApiField.REASON, PhotoUpload.REASON_SIZE));
            }
            Path target = null;
            try {
                byte[] bytes = photo.getBytes();
                String extension = detectImageExtension(bytes);
                if (extension == null) {
                    throw new BusinessException(
                            ErrorCode.PHOTO_INVALID,
                            HttpStatus.BAD_REQUEST,
                            Map.of(ApiField.REASON, PhotoUpload.REASON_TYPE));
                }
                Files.createDirectories(uploadDirectory);
                target = uploadDirectory.resolve(UUID.randomUUID() + "." + extension);
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
                Path savedFile = target;
                // CSDL rollback thì file cũng phải được dọn, tránh file mồ côi.
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(int status) {
                                if (status != STATUS_COMMITTED) {
                                    deleteQuietly(savedFile);
                                }
                            }
                        });
                var row = new MaintenancePhoto();
                row.setRequestId(requestId);
                row.setFilePath(target.getFileName().toString());
                row.setUploadedAt(OffsetDateTime.now());
                repository.save(row);
            } catch (IOException e) {
                if (target != null) {
                    deleteQuietly(target);
                }
                throw new BusinessException(
                        ErrorCode.PHOTO_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override()
    public Path getPhotoFile(Long requestId, Long photoId) {
        var photo = repository.findById(photoId).orElseThrow(BusinessException::notFound);
        if (!requestId.equals(photo.getRequestId())) {
            throw BusinessException.notFound();
        }
        Path file = uploadDirectory.resolve(photo.getFilePath()).normalize();
        if (!file.startsWith(uploadDirectory) || !Files.isRegularFile(file)) {
            throw BusinessException.notFound();
        }
        return file;
    }

    private String detectImageExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) {
            return "jpg";
        }
        if (b.length >= 8
                && Arrays.equals(
                        Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})) {
            return "png";
        }
        if (b.length >= 12
                && new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "webp";
        }
        return null;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Không dọn được ảnh rollback {}", path, e);
        }
    }
}
