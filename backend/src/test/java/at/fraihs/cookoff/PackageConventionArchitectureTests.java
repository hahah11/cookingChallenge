package at.fraihs.cookoff;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import org.mapstruct.Mapper;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.RECORDS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.TOP_LEVEL_CLASSES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the mapper/dto/model/entity package convention from
 * {@code docs/backend/03-code-style.md} (see ADR 0004), so the split it fixed cannot silently
 * reappear.
 */
@AnalyzeClasses(packages = "at.fraihs.cookoff", importOptions = {
        ImportOption.DoNotIncludeTests.class,
        PackageConventionArchitectureTests.DoNotIncludeGeneratedOpenApi.class
})
class PackageConventionArchitectureTests {

    @ArchTest
    static final ArchRule mapperNamedClassesResideInMapperPackage = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().resideInAPackage("..mapper..")
            .because("mappers live in the mapper/ package of the layer they map for, per docs/backend/03-code-style.md");

    @ArchTest
    static final ArchRule mapstructMappersResideInMapperPackage = classes()
            .that().areAnnotatedWith(Mapper.class)
            .should().resideInAPackage("..mapper..")
            .because("mappers live in the mapper/ package of the layer they map for, per docs/backend/03-code-style.md");

    @ArchTest
    static final ArchRule jpaEntitiesResideInEntityPackage = classes()
            .that().areAnnotatedWith(Entity.class).or().areAnnotatedWith(Embeddable.class)
            .should().resideInAPackage("..entity..")
            .because("JPA entities and embeddables live in the entity/ package, per docs/backend/03-code-style.md");

    @ArchTest
    static final ArchRule topLevelRecordsDoNotLiveInServiceOrPortPackages = noClasses()
            .that(RECORDS).and(TOP_LEVEL_CLASSES)
            .should().resideInAnyPackage("..application.service..", "..application.port..", "..domain.service..")
            .because("data types live in dto/ (application), model/ (domain), or entity/ (JPA), never in service/ or "
                    + "port/, per docs/backend/03-code-style.md — a use-case-private nested record is the one "
                    + "documented exemption");

    static class DoNotIncludeGeneratedOpenApi implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/openapi/");
        }
    }
}
