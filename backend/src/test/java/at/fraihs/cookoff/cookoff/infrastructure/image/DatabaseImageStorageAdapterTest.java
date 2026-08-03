package at.fraihs.cookoff.cookoff.infrastructure.image;

import at.fraihs.cookoff.cookoff.application.exception.ChallengeImageNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.StoredImage;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseImageStorageAdapterTest {

    @Autowired
    private ChallengeImageJpaRepository jpaRepository;

    private DatabaseImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DatabaseImageStorageAdapter(jpaRepository);
    }

    @Test
    void should_roundTripBlob_when_storingThenResolving() {
        byte[] bytes = {1, 2, 3, 4, 5};

        String imageRef = adapter.store(bytes, "image/png");
        StoredImage resolved = adapter.resolve(imageRef);

        assertArrayEquals(bytes, resolved.bytes());
        assertEquals("image/png", resolved.contentType());
    }

    @Test
    void should_throw_when_imageRefIsUnknown() {
        String neverStoredRef = TsidSupport.toBase32(TsidSupport.generate());

        assertThrows(ChallengeImageNotFoundException.class, () -> adapter.resolve(neverStoredRef));
    }

    @Test
    void should_makeImageUnresolvable_when_deleted() {
        String imageRef = adapter.store(new byte[] {9}, "image/jpeg");

        adapter.delete(imageRef);

        assertThrows(ChallengeImageNotFoundException.class, () -> adapter.resolve(imageRef));
    }
}
