package com.knowledgehub;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Compiler-enforced version of the dependency rule in docs/development/ARCHITECTURE.md: inside each
 * module the layers are the {@code domain} / {@code application} / {@code infrastructure} packages,
 * and dependencies only point inward. ArchUnit reads the compiled classes once and checks every
 * {@code @ArchTest} rule against them; a violation fails the build with the offending import in the
 * message.
 */
@AnalyzeClasses(packages = "com.knowledgehub", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

  @ArchTest
  static final ArchRule domainDependsOnlyInward =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..application..", "..infrastructure..")
          .because("domain is the innermost layer; it must not know who orchestrates or stores it");

  @ArchTest
  static final ArchRule domainStaysFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "org.neo4j..", "io.qdrant..")
          .because("domain is plain Java; frameworks belong to adapters (infrastructure)");

  @ArchTest
  static final ArchRule applicationDependsOnlyInward =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..infrastructure..")
          .because(
              "application is the middle layer; it must not know who stores or presents it (infrastructure)");
}
