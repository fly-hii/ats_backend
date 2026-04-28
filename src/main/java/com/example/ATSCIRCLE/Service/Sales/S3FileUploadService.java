package com.example.ATSCIRCLE.Service.Sales;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service("s3FileUploadServiceSales")
public class S3FileUploadService {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Upload file to S3 and return the file URL
     */
    public String uploadFile(MultipartFile file, String organizationId) throws IOException {
        String fileName = generateUniqueFileName(file.getOriginalFilename(), organizationId);
        
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    fileName,
                    file.getInputStream(),
                    null
            );

            PutObjectResult result = amazonS3.putObject(putObjectRequest);

            // Generate and return the S3 URL
            return amazonS3.getUrl(bucketName, fileName).toString();
        } catch (IOException e) {
            throw new IOException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Upload multiple files to S3
     */
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String organizationId) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return uploadedUrls;
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileUrl = uploadFile(file, organizationId);
                uploadedUrls.add(fileUrl);
            }
        }

        return uploadedUrls;
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            amazonS3.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            System.err.println("Failed to delete file from S3: " + e.getMessage());
        }
    }

    /**
     * Generate unique file name with organization ID and timestamp
     */
    private String generateUniqueFileName(String originalFileName, String organizationId) {
        String timestamp = System.currentTimeMillis() + "";
        String fileExtension = getFileExtension(originalFileName);
        return "email-attachments/" + organizationId + "/" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;
    }

    /**
     * Extract file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * Extract file name from S3 URL
     */
    private String extractFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
