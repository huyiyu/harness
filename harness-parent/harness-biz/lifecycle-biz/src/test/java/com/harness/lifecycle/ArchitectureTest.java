package com.harness.lifecycle;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.harness.lifecycle")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllerShouldNotCallMapper =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..mapper..");

    @ArchTest
    static final ArchRule transactionalOnlyInService =
        noMethods().that().areDeclaredInClassesThat()
            .resideOutsideOfPackage("..service..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional");

    @ArchTest
    static final ArchRule noClassForName =
        noClasses().should().callMethod(Class.class, "forName", String.class);
}
