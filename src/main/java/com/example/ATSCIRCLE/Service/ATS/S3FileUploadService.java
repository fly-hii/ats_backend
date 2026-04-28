
package com.example.ATSCIRCLE.Service.ATS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service("s3FileUploadServiceATS")
public class S3FileUploadService {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    // Allowed file extensions
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "txt"
    );
    
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Upload file to S3 and return the URL
     */
    public String uploadFile(MultipartFile file, String folderName, String organizationId) throws IOException {
        // Validate file
        validateFile(file);
        
        // Generate unique file name
        String fileName = generateFileName(file.getOriginalFilename(), organizationId);
        String key = folderName + "/" + organizationId + "/" + fileName;
        
        // Upload to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        try (InputStream inputStream = file.getInputStream()) {
    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);
    amazonS3.putObject(putObjectRequest);
}
        
        // Return the S3 URL
        return getFileUrl(key);
    }

    /**
     * Upload resume file
     */
    public String uploadResume(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "resumes", organizationId);
    }

    /**
     * Upload cover letter
     */
    public String uploadCoverLetter(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "cover-letters", organizationId);
    }

    /**
     * Upload profile photo
     */
    public String uploadProfilePhoto(MultipartFile file, String organizationId) throws IOException {
        validateImageFile(file);
        return uploadFile(file, "profile-photos", organizationId);
    }

    /**
     * Upload professional licenses
     */
    public String uploadProfessionalLicenses(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "professional-licenses", organizationId);
    }

    /**
     * Upload certifications
     */
    public String uploadCertifications(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "certifications", organizationId);
    }

    /**
     * Upload supporting documents
     */
    public String uploadSupportingDocuments(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "supporting-documents", organizationId);
    }

    /**
     * Upload area professional certifications
     */
    public String uploadAreaProfessionalCertifications(MultipartFile file, String organizationId) throws IOException {
        return uploadFile(file, "area-certifications", organizationId);
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key != null && !key.isEmpty()) {
                amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
            }
        } catch (Exception e) {
            // Log error but don't throw exception
            System.err.println("Error deleting file from S3: " + e.getMessage());
        }
    }

    /**
     * Generate unique file name
     */
    private String generateFileName(String originalFileName, String organizationId) {
        String extension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        return timestamp + "_" + uniqueId + "." + extension;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Validate file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension) && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IOException("File type not allowed. Allowed types: " + 
                    String.join(", ", ALLOWED_DOCUMENT_EXTENSIONS) + ", " + 
                    String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IOException("File type not allowed. Allowed image types: " + 
                    String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }
    }

    /**
     * Get file URL from S3 key
     */
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * Extract S3 key from URL
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        try {
            String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (fileUrl.startsWith(baseUrl)) {
                return fileUrl.substring(baseUrl.length());
            }
        } catch (Exception e) {
            System.err.println("Error extracting key from URL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key != null && !key.isEmpty()) {
                return amazonS3.doesObjectExist(bucketName, key);
            }
        } catch (Exception e) {
            System.err.println("Error checking file existence: " + e.getMessage());
        }
        return false;
    }
}