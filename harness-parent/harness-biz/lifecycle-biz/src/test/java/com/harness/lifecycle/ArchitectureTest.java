package com.harness.lifecycle;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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

    @ArchTest
    static final ArchRule noMethodInvoke =
        noClasses().should().callMethod(java.lang.reflect.Method.class, "invoke", Object.class, Object[].class);

    @ArchTest
    static final ArchRule utilOnlyInApiCommon =
        noClasses().that().resideInAPackage("..util..")
            .should().resideOutsideOfPackage("com.harness.api.common.util..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule starterConfigOnlyViaSpi =
        noClasses().that().resideInAPackage("..config..")
            .should().beAnnotatedWith("org.springframework.context.annotation.ComponentScan");

    // 包名/实现方式约束
    @ArchTest
    static final ArchRule restControllerOnlyInControllerPackage =
        classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAPackage("..controller..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceOnlyInServicePackage =
        classes().that().areAnnotatedWith("org.springframework.stereotype.Service")
            .should().resideInAPackage("..service..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule enumsOnlyInEnumsPackage =
        classes().that().areEnums()
            .should().resideInAPackage("..enums..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule constantsOnlyInConstantPackage =
        classes().that().areInterfaces()
            .and().haveSimpleNameEndingWith("Constant")
            .should().resideInAPackage("..constant..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule mapperOnlyInMapperPackage =
        classes().that().areAnnotatedWith("org.apache.ibatis.annotations.Mapper")
            .should().resideInAPackage("..mapper..")
            .allowEmptyShould(true);
}
