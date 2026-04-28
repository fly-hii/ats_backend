
package com.example.ATSCIRCLE.Service.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Upload file to S3 and return the URL
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = folder + "/" + UUID.randomUUID().toString() + fileExtension;

        // Set metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // Upload to S3
        PutObjectRequest putObjectRequest = new PutObjectRequest(
            bucketName,
            uniqueFileName,
            file.getInputStream(),
            metadata
        );

        amazonS3.putObject(putObjectRequest);

        // Return the S3 URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s", 
                           bucketName, region, uniqueFileName);
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract key from URL
            String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            amazonS3.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage());
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean doesFileExist(String fileUrl) {
        try {
            String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            return amazonS3.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            return false;
        }
    }
}