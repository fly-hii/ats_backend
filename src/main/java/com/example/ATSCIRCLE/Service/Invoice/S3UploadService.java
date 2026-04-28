package com.example.ATSCIRCLE.Service.Invoice;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;

@Service
public class S3UploadService {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    /**
     * Upload file to S3 and return the file URL
     * 
     * @param file - MultipartFile from frontend
     * @param organizationId - Organization ID for folder structure
     * @param documentType - Type of document (e.g., "offer_letter", "contract", "invoice")
     * @return S3 file URL (now returns presigned URL valid for 7 days)
     */
    public String uploadFile(MultipartFile file, String organizationId, String documentType) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Create folder structure: organizations/{orgId}/invoices/{documentType}/{filename}
            String s3Key = String.format("organizations/%s/invoices/%s/%s", 
                organizationId, documentType, uniqueFilename);

            // Set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("original-filename", originalFilename);
            metadata.addUserMetadata("organization-id", organizationId);
            metadata.addUserMetadata("document-type", documentType);

            // Upload file to S3
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName, 
                s3Key, 
                inputStream, 
                metadata
            );
            
            // ✅ NO ACL - Bucket policy will handle public access
            // Don't set any ACL when bucket has ACLs disabled
            
            amazonS3.putObject(putObjectRequest);
            inputStream.close();

            // ✅ Return permanent public URL (works with bucket policy)
            return amazonS3.getUrl(bucketName, s3Key).toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Upload multiple files
     */
    public String[] uploadMultipleFiles(MultipartFile[] files, String organizationId, String documentType) {
        String[] urls = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = uploadFile(files[i], organizationId, documentType);
        }
        return urls;
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract S3 key from URL
            String s3Key = extractS3KeyFromUrl(fileUrl);
            amazonS3.deleteObject(bucketName, s3Key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NEW: Generate presigned URL directly from S3 key
     * @param s3Key - S3 object key
     * @param validityMinutes - Validity period in minutes
     * @return Presigned URL
     */
    public String generatePresignedUrlFromKey(String s3Key, int validityMinutes) {
        try {
            // java.util.Date expiration = new java.util.Date();
            // long expTimeMillis = expiration.getTime();
            // expTimeMillis += 1000L * 60 * validityMinutes; // Convert minutes to milliseconds
            // expiration.setTime(expTimeMillis);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = 
                new GeneratePresignedUrlRequest(bucketName, s3Key)
                    .withMethod(com.amazonaws.HttpMethod.GET);
                    // .withExpiration(expiration);

            return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * Generate presigned URL for temporary access
     * @param fileUrl - Full S3 URL or presigned URL
     * @param validityMinutes - Validity period in minutes (default: 60 minutes / 1 hour)
     */
    public String generatePresignedUrl(String fileUrl, int validityMinutes) {
        try {
            String s3Key = extractS3KeyFromUrl(fileUrl);
            return generatePresignedUrlFromKey(s3Key, validityMinutes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * Overloaded method - defaults to 1 hour validity
     */
    public String generatePresignedUrl(String fileUrl) {
        return generatePresignedUrl(fileUrl, 60); // 1 hour
    }

    /**
     * ✅ NEW: Refresh presigned URL (useful for expired URLs)
     * Generates a new presigned URL with 7 days validity
     */
    public String refreshPresignedUrl(String fileUrl) {
        return generatePresignedUrl(fileUrl, 7 * 24 * 60); // 7 days
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String fileUrl) {
        try {
            String s3Key = extractS3KeyFromUrl(fileUrl);
            return amazonS3.doesObjectExist(bucketName, s3Key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file metadata
     */
    public ObjectMetadata getFileMetadata(String fileUrl) {
        try {
            String s3Key = extractS3KeyFromUrl(fileUrl);
            return amazonS3.getObjectMetadata(bucketName, s3Key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Download file from S3 and convert to MultipartFile for email attachment
     * ✅ EMAIL RESEND KE LIYE - S3 nundi files download chesi MultipartFile ga convert
     * Uses AWS SDK v1 (com.amazonaws)
     */
    public MultipartFile downloadFileAsMultipart(String fileUrl) {
        try {
            // Extract S3 key from URL
            String s3Key = extractS3KeyFromUrl(fileUrl);
            
            // Download file from S3 using AWS SDK v1
            S3Object s3Object = amazonS3.getObject(bucketName, s3Key);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            
            // Read all bytes from input stream
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
            byte[] fileContent = buffer.toByteArray();
            
            // Close streams
            inputStream.close();
            buffer.close();
            
            // Get original filename from S3 key
            String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);
            
            // Get content type from S3 metadata
            ObjectMetadata metadata = s3Object.getObjectMetadata();
            String contentType = metadata.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            System.out.println("✅ Downloaded file from S3: " + filename + " (" + fileContent.length + " bytes)");
            
            // Create MultipartFile from byte array
            return new CustomMultipartFile(fileContent, filename, contentType);
            
        } catch (Exception e) {
            System.err.println("❌ Error downloading file from S3: " + fileUrl + " - " + e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    /**
     * Custom MultipartFile implementation for S3 downloaded files
     * ✅ Byte array ni MultipartFile ga convert - Email attachment ke liye
     */
    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] fileContent;
        private final String fileName;
        private final String contentType;
        
        public CustomMultipartFile(byte[] fileContent, String fileName, String contentType) {
            this.fileContent = fileContent;
            this.fileName = fileName;
            this.contentType = contentType;
        }
        
        @Override
        public String getName() {
            return fileName;
        }
        
        @Override
        public String getOriginalFilename() {
            return fileName;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return fileContent == null || fileContent.length == 0;
        }
        
        @Override
        public long getSize() {
            return fileContent.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return fileContent;
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(fileContent);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(fileContent);
            }
        }
    }

    // Helper methods
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractS3KeyFromUrl(String fileUrl) {
        // Extract key from presigned URL or regular S3 URL
        try {
            // Handle presigned URLs (remove query parameters)
            String urlWithoutParams = fileUrl.split("\\?")[0];
            
            // Format 1: https://bucket-name.s3.region.amazonaws.com/key
            if (urlWithoutParams.contains(bucketName + ".s3")) {
                String[] parts = urlWithoutParams.split(bucketName + ".s3.[^/]+.amazonaws.com/");
                if (parts.length > 1) {
                    return parts[1];
                }
            }
            
            // Format 2: https://s3.region.amazonaws.com/bucket-name/key
            if (urlWithoutParams.contains("s3.") && urlWithoutParams.contains(bucketName)) {
                String[] parts = urlWithoutParams.split(bucketName + "/");
                if (parts.length > 1) {
                    return parts[1];
                }
            }
            
            // Format 3: Simple extraction after .com/
            String[] parts = urlWithoutParams.split(".com/");
            if (parts.length > 1) {
                return parts[1];
            }
            
            throw new RuntimeException("Invalid S3 URL format");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract S3 key from URL: " + fileUrl, e);
        }
    }
}