package at.fraihs.cookoff;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(CookoffApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
