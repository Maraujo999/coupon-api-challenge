package com.maraujo.couponapi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.maraujo.couponapi",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule coreIsIndependent =
      noClasses()
          .that()
          .resideInAnyPackage("..coupon.domain..", "..coupon.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "org.hibernate..",
              "com.fasterxml..",
              "tools.jackson..",
              "..adapter..",
              "..config..",
              "..infrastructure..",
              "..auth..",
              "..shared..");
}
