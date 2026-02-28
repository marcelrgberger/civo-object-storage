package de.bergerrosenstock.civo;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CivoObjectStorage {
    private final MinioClient minio;
    private final String bucket;
    private final String endpoint;

    public static final String ENDPOINT_FRA_1 = "https://objectstore.fra1.civo.com";

    public CivoObjectStorage(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.minio = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Returns the bucket name configured for this storage instance.
     *
     * @return the bucket name
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Returns the endpoint URL configured for this storage instance.
     *
     * @return the endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Uploads a byte array to the object storage with the specified key and content type.
     *
     * @param key         the key to associate with the object in the storage
     * @param bytes       the byte array representing the data to store
     * @param contentType the MIME type of the stored object
     * @throws CivoObjectStorageException if an error occurs during the upload process
     */
    public ObjectWriteResponse putBytes(String key, byte[] bytes, String contentType) throws CivoObjectStorageException {
        return putBytes(key, bytes, contentType, null);
    }

    /**
     * Uploads a byte array to the object storage with the specified key, content type,
     * and optional user-defined metadata.
     *
     * @param key         the key to associate with the object in the storage
     * @param bytes       the byte array representing the data to store
     * @param contentType the MIME type of the stored object
     * @param userMeta    a map of user-defined metadata to associate with the object, can be null
     * @throws CivoObjectStorageException if an error occurs during the upload process
     */
    public ObjectWriteResponse putBytes(String key, byte[] bytes, String contentType, Map<String, String> userMeta) throws CivoObjectStorageException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            PutObjectArgs.Builder b = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, bytes.length, -1)
                    .contentType(contentType);
            if (userMeta != null && !userMeta.isEmpty()) {
                b.userMetadata(userMeta);
            }
            return minio.putObject(b.build());
        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while put bytes to key %s", key), e);
        }
    }

    /**
     * Uploads an input stream to the object storage with the specified key and content type.
     *
     * @param key         the key to associate with the object in the storage
     * @param inputStream the input stream to upload
     * @param size        the size of the input stream in bytes, or -1 if unknown
     * @param contentType the MIME type of the stored object
     * @throws CivoObjectStorageException if an error occurs during the upload process
     */
    public ObjectWriteResponse putStream(String key, InputStream inputStream, long size, String contentType) throws CivoObjectStorageException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build();
            return minio.putObject(args);
        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while put stream to key %s", key), e);
        }
    }

    /**
     * Deletes an object from the storage using the specified key.
     *
     * @param key the key identifying the object to delete
     * @throws CivoObjectStorageException if an error occurs while deleting the object
     */
    public void deleteObject(String key) throws CivoObjectStorageException {
        try {
            minio.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while deleting key %s from storage", key), e);
        }
    }

    /**
     * Retrieves an object from the storage using the specified key.
     *
     * @param key the key identifying the object to retrieve
     * @return a StoredObject containing the data, content type, and metadata of the retrieved object
     * @throws CivoObjectStorageException if an error occurs while retrieving the object
     */
    public StoredObject getObject(String key) throws CivoObjectStorageException {
        try {
            StatObjectResponse stat = minio.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            String contentType = stat.contentType();
            Map<String, String> userMeta = stat.userMetadata();

            try (InputStream is = minio.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            )) {
                byte[] data = is.readAllBytes();
                return new StoredObject(data, contentType, userMeta);
            }
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while get object %s with key", key), e);
        }
    }

    /**
     * Checks whether an object with the specified key exists in the storage.
     *
     * @param key the key identifying the object to check
     * @return true if the object exists, false otherwise
     */
    public boolean objectExists(String key) {
        try {
            minio.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the direct public URL for an object (endpoint/bucket/key).
     * Note: the object must be publicly accessible for this URL to work without authentication.
     *
     * @param key the key identifying the object
     * @return the public URL
     */
    public String getObjectUrl(String key) {
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return normalizedEndpoint + "/" + bucket + "/" + key;
    }

    /**
     * Generates a pre-signed URL for temporary access to an object.
     *
     * @param key     the key identifying the object
     * @param expiry  the duration value for the pre-signed URL
     * @param unit    the time unit for the expiry duration
     * @return the pre-signed URL
     * @throws CivoObjectStorageException if an error occurs while generating the URL
     */
    public String getPresignedUrl(String key, int expiry, TimeUnit unit) throws CivoObjectStorageException {
        try {
            return minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expiry, unit)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while generating presigned URL for key %s", key), e);
        }
    }

    /**
     * Returns metadata (stat) for an object without downloading it.
     *
     * @param key the key identifying the object
     * @return the stat response containing size, content type, etag, etc.
     * @throws CivoObjectStorageException if an error occurs while retrieving metadata
     */
    public StatObjectResponse statObject(String key) throws CivoObjectStorageException {
        try {
            return minio.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new CivoObjectStorageException(String.format("Error while stat object %s", key), e);
        }
    }

    public record StoredObject(byte[] data, String contentType, Map<String, String> userMetadata) {
    }

    public static final class ContentTypes {
        private ContentTypes() {
        }

        // Generisch / Binär
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

        // Text
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String TEXT_CSS = "text/css";
        public static final String TEXT_CSV = "text/csv";
        public static final String TEXT_XML = "text/xml";
        public static final String TEXT_MARKDOWN = "text/markdown";

        // JSON / XML
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_XML = "application/xml";

        // JavaScript
        public static final String APPLICATION_JAVASCRIPT = "application/javascript";

        // Bilder
        public static final String IMAGE_PNG = "image/png";
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_WEBP = "image/webp";
        public static final String IMAGE_SVG_XML = "image/svg+xml";
        public static final String IMAGE_HEIC = "image/heic";
        public static final String IMAGE_HEIF = "image/heif";

        // Audio / Video
        public static final String AUDIO_MPEG = "audio/mpeg";
        public static final String AUDIO_OGG = "audio/ogg";
        public static final String AUDIO_WAV = "audio/wav";
        public static final String VIDEO_MP4 = "video/mp4";
        public static final String VIDEO_WEBM = "video/webm";

        // Dokumente
        public static final String APPLICATION_PDF = "application/pdf";
        public static final String APPLICATION_MSWORD = "application/msword";
        public static final String APPLICATION_DOCX =
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final String APPLICATION_MSEXCEL = "application/vnd.ms-excel";
        public static final String APPLICATION_XLSX =
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        public static final String APPLICATION_MSPPT = "application/vnd.ms-powerpoint";
        public static final String APPLICATION_PPTX =
                "application/vnd.openxmlformats-officedocument.presentationml.presentation";

        // Archive / Kompression
        public static final String APPLICATION_ZIP = "application/zip";
        public static final String APPLICATION_TAR = "application/x-tar";
        public static final String APPLICATION_GZIP = "application/gzip";

        // Fonts
        public static final String FONT_WOFF = "font/woff";
        public static final String FONT_WOFF2 = "font/woff2";

        // NDJSON
        public static final String APPLICATION_NDJSON = "application/x-ndjson";

        /**
         * Appends "; charset=utf-8" to textual content types if not already present.
         *
         * @param contentType the content type string
         * @return the content type with charset appended if applicable
         */
        public static String withUtf8(String contentType) {
            if (contentType == null || contentType.isBlank()) return contentType;
            if (contentType.startsWith("text/")
                    || contentType.equals(APPLICATION_JSON)
                    || contentType.equals(APPLICATION_XML)
                    || contentType.equals(TEXT_XML)
                    || contentType.equals(TEXT_CSV)
                    || contentType.equals(TEXT_HTML)
                    || contentType.equals(TEXT_PLAIN)
                    || contentType.equals(TEXT_MARKDOWN)) {
                return contentType.contains("charset=") ? contentType : contentType + "; charset=utf-8";
            }
            return contentType;
        }

        /**
         * Checks whether the given content type represents textual content.
         *
         * @param contentType the content type string
         * @return true if the content type is textual
         */
        public static boolean isTextual(String contentType) {
            if (contentType == null) return false;
            return contentType.startsWith("text/")
                    || contentType.equals(APPLICATION_JSON)
                    || contentType.equals(APPLICATION_XML)
                    || contentType.equals(TEXT_XML);
        }

        /**
         * Checks whether the given content type represents an image.
         *
         * @param contentType the content type string
         * @return true if the content type is an image
         */
        public static boolean isImage(String contentType) {
            if (contentType == null) return false;
            return contentType.startsWith("image/");
        }
    }
}
