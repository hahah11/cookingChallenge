package at.fraihs.cookoff;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.jmolecules.archunit.JMoleculesDddRules;

/**
 * Verifies the jMolecules DDD stereotypes (@AggregateRoot, @ValueObject, @Repository, ...) are
 * actually honored, not just documented.
 * <p>
 * Note: jMolecules' own {@code JMoleculesArchitectureRules.ensureLayering()} assumes the classic
 * top-down stack where Domain is allowed to call Infrastructure directly. This project instead
 * applies Dependency Inversion (Infrastructure implements Domain's Repository ports, e.g.
 * ChallengeRepositoryImpl -> ChallengeRepository), so that rule would fail against our actual,
 * intended architecture. {@link #dependenciesPointInward} encodes the real Dependency Rule instead.
 */
@AnalyzeClasses(packages = "at.fraihs.cookoff", importOptions = ImportOption.DoNotIncludeTests.class)
class JMoleculesArchitectureTests {

    @ArchTest
    static final ArchRule dddRulesAreRespected = JMoleculesDddRules.all();

    @ArchTest
    static final ArchRule dependenciesPointInward = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Interfaces").definedBy("..interfaces..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Domain").mayOnlyAccessLayers("Domain")
            .whereLayer("Application").mayOnlyAccessLayers("Domain", "Application")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application", "Infrastructure")
            .whereLayer("Interfaces").mayOnlyAccessLayers("Domain", "Application", "Interfaces");
}
