import de.bergerrosenstock.civo.CivoObjectStorage;
import de.bergerrosenstock.civo.CivoObjectStorageException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class CivoTest {

    static String accessKey = "D8EWJPLSU66DSR8DRU5D";
    static String secretKey = "pB5L4yHDPsu0pMyqoJMmiSJ1XlPaPs2IxE07Om6F";
    static String bucketName = "dokuai-dev";
    static CivoObjectStorage storage;
    static String key = "test";

    @BeforeAll
    static void setUp() {
        storage = new CivoObjectStorage(CivoObjectStorage.ENDPOINT_FRA_1, accessKey, secretKey, bucketName);
    }

    @Test
    @Order(1)
    public void put() throws Exception {
        byte[] bytes = new byte[1024];
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", "TestName");
        storage.putBytes(key, bytes, CivoObjectStorage.ContentTypes.APPLICATION_OCTET_STREAM, metadata
        );

    }


    @Test
    @Order(2)
    public void get() throws CivoObjectStorageException {

        CivoObjectStorage.StoredObject object = storage.getObject(key);
        System.out.println( object.userMetadata());


    }

    @Test
    @Order(3)
    public void delete() throws CivoObjectStorageException {
        storage.deleteObject(key);
    }
}
