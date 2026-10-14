package vn.edu.tlu.dms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Kiểm tra quy tắc phụ thuộc giữa các module (SAD mục 4.2). Test đỏ nghĩa là có lời gọi sai chiều:
 * chuyển sang dùng sự kiện, hoặc gọi Service công khai của module tầng thấp hơn.
 */
@AnalyzeClasses(packages = "vn.edu.tlu.dms", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleDependencyTest {

    /**
     * Thứ tự tầng từ thấp lên. Module chỉ được gọi module ở tầng thấp hơn; cùng tầng không gọi
     * nhau.
     */
    private static final List<List<String>> TIERS =
            List.of(
                    List.of("common"),
                    List.of("account"),
                    List.of("room"),
                    List.of("billing"),
                    List.of("contract"),
                    List.of("registration", "maintenance"),
                    List.of("notification"));

    private static final List<String> MODULES = TIERS.stream().flatMap(List::stream).toList();

    private static String pkg(String module) {
        return "vn.edu.tlu.dms." + module + "..";
    }

    @ArchTest
    void modulesOnlyCallLowerTiers(JavaClasses classes) {
        callsUpward().forEach(rule -> rule.check(classes));
    }

    /** Không dùng Repository hoặc Controller / DTO của module khác; đi qua Service công khai. */
    @ArchTest
    void noForeignRepositoryOrApi(JavaClasses classes) {
        foreignLayer("repository").forEach(rule -> rule.check(classes));
        foreignLayer("api").forEach(rule -> rule.check(classes));
    }

    /** Service chỉ khai báo hợp đồng; lớp có logic phải nằm trong service.impl. */
    @ArchTest
    void serviceContractsHaveNoImplementation(JavaClasses classes) {
        for (var type : classes) {
            if (type.getPackageName().endsWith(".service") && !type.getName().contains("$")) {
                assertThat(type.isInterface())
                        .as("%s phải là interface, chuyển logic vào service.impl", type.getName())
                        .isTrue();
                for (var method : type.getMethods()) {
                    // Java tự sinh bridge method cho interface CRUD generic; đây không phải logic
                    // viết trong Service.
                    if (method.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                        continue;
                    }
                    assertThat(method.getModifiers())
                            .as(
                                    "%s chỉ được khai báo hàm, không có default/static body",
                                    method.getFullName())
                            .contains(JavaModifier.ABSTRACT);
                }
            }
        }
    }

    /** Bean Service phải là Impl và thực hiện một hợp đồng interface. */
    @ArchTest
    void serviceBeansImplementInterfacesInImplPackage(JavaClasses classes) {
        for (var type : classes) {
            if (type.isAnnotatedWith(Service.class)) {
                assertThat(type.getPackageName()).endsWith(".service.impl");
                assertThat(type.getSimpleName()).endsWith("Impl");
                assertThat(type.getAllRawInterfaces())
                        .as("%s phải implements một interface Service", type.getName())
                        .isNotEmpty();
            }
        }
    }

    /** API mới phải khai quyền tường minh; tránh bỏ sót annotation hoặc gõ sai mã quyền. */
    @ArchTest
    void restEndpointsDeclareRegisteredPrivileges(JavaClasses classes) {
        List<String> registeredPrivileges = ScopeGuard.permissionsForRole(DmsConstants.Role.ADMIN);
        int checkedEndpoints = 0;
        for (var type : classes) {
            if (!type.isAnnotatedWith(RestController.class)) {
                continue;
            }
            Class<?> controller = type.reflect();
            assertThat(Modifier.isFinal(controller.getModifiers()))
                    .as("%s không được final để Spring proxy kiểm tra quyền", controller.getName())
                    .isFalse();
            for (var method : controller.getMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                assertThat(Modifier.isFinal(method.getModifiers()))
                        .as("%s không được final để annotation phân quyền có hiệu lực", method)
                        .isFalse();
                RequirePrivilege requirement =
                        AnnotatedElementUtils.findMergedAnnotation(method, RequirePrivilege.class);
                if (requirement == null) {
                    requirement =
                            AnnotatedElementUtils.findMergedAnnotation(
                                    controller, RequirePrivilege.class);
                }
                assertThat(requirement)
                        .as("%s phải khai @RequirePrivilege ở hàm hoặc lớp", method)
                        .isNotNull();
                assertThat(requirement.value())
                        .as("%s phải khai ít nhất một mã quyền hợp lệ", method)
                        .isNotEmpty()
                        .allMatch(registeredPrivileges::contains);
                checkedEndpoints++;
            }
        }
        assertThat(checkedEndpoints)
                .as("Phải kiểm tra được các REST endpoint thực tế")
                .isPositive();
    }

    /**
     * Baseline được chụp từ bản đã kiểm thử trước khi gom Constants. Giữ mã wire và ma trận quyền
     * cũ: một lần refactor không được vô tình cấp thêm hoặc thu hồi quyền của bất kỳ role nào. Chỉ
     * cập nhật baseline khi có yêu cầu thay đổi chính sách phân quyền được duyệt.
     */
    @ArchTest
    void constantsRefactorKeepsExistingRolePrivileges(JavaClasses classes) throws Exception {
        Map<String, Integer> expectedCounts =
                Map.of("ADMIN", 103, "STAFF", 72, "STUDENT", 35, "TECHNICIAN", 11);
        Map<String, String> expectedDigests =
                Map.of(
                        "ADMIN", "483bf515ba32c79a31bfcf5a5b867bf98b31a8836153cdfa3b54e8d3034749ce",
                        "STAFF", "e2bbba0bfe43fc61920463cd513da6bf7ae2626509d12d805b10459fdebe1b4d",
                        "STUDENT",
                                "8f6ded25810fce60f71926b83efe9b1b3cb8b52a492dd9c836672fc794d97ca3",
                        "TECHNICIAN",
                                "07426f36cdf2721233c22d2f536c59e1cf2f5415a9eb832ee4fe7f1924a09c75");
        assertThat(DmsConstants.Role.ALL)
                .containsExactlyInAnyOrderElementsOf(expectedCounts.keySet());
        for (var baseline : expectedCounts.entrySet()) {
            List<String> permissions = ScopeGuard.permissionsForRole(baseline.getKey());
            assertThat(permissions)
                    .as("Số quyền của %s phải giữ nguyên sau khi gom Constants", baseline.getKey())
                    .hasSize(baseline.getValue())
                    .doesNotHaveDuplicates()
                    .isSorted();
            byte[] payload = String.join("\n", permissions).getBytes(StandardCharsets.UTF_8);
            String digest =
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
            assertThat(digest)
                    .as(
                            "Danh sách mã quyền của %s phải khớp baseline đã kiểm thử",
                            baseline.getKey())
                    .isEqualTo(expectedDigests.get(baseline.getKey()));
        }
        assertThat(ScopeGuard.permissionsForRole("UNREGISTERED_ROLE")).isEmpty();
        assertThat(ScopeGuard.permissionsForRole("admin")).isEmpty();
        assertThat(ScopeGuard.permissionsForRole("")).isEmpty();
    }

    /**
     * Catalog Constants phải đủ và không trùng mã. ADMIN được cấp toàn bộ catalog; các role khác
     * chỉ nhận một phần của catalog, không được tự sinh mã quyền ngoài danh sách đã đăng ký.
     */
    @ArchTest
    void permissionConstantsMatchRegisteredCatalog(JavaClasses classes) throws Exception {
        List<String> declaredCodes = new ArrayList<>();
        for (Field field : DmsConstants.Permission.class.getDeclaredFields()) {
            if (field.getType() != String.class || !Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            assertThat(
                            Modifier.isStatic(field.getModifiers())
                                    && Modifier.isFinal(field.getModifiers()))
                    .as("%s phải là hằng compile-time để dùng trong annotation", field.getName())
                    .isTrue();
            String code = (String) field.get(null);
            assertThat(code)
                    .as("Mã quyền %s phải giữ đúng tên công khai của nó", field.getName())
                    .isEqualTo(field.getName());
            declaredCodes.add(code);
        }
        assertThat(declaredCodes)
                .hasSize(103)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(DmsConstants.Permission.ALL);
        assertThat(ScopeGuard.permissionsForRole(DmsConstants.Role.ADMIN))
                .containsExactlyInAnyOrderElementsOf(DmsConstants.Permission.ALL);
        for (String role : DmsConstants.Role.ALL) {
            assertThat(ScopeGuard.permissionsForRole(role))
                    .as("%s chỉ được dùng mã đã đăng ký trong Constants", role)
                    .allMatch(DmsConstants.Permission.ALL::contains);
        }
    }

    private static List<ArchRule> callsUpward() {
        List<ArchRule> rules = new ArrayList<>();
        for (int i = 0; i < TIERS.size(); i++) {
            for (String module : TIERS.get(i)) {
                List<String> forbidden = new ArrayList<>();
                for (int j = i; j < TIERS.size(); j++) {
                    for (String other : TIERS.get(j)) {
                        if (!other.equals(module)) {
                            forbidden.add(pkg(other));
                        }
                    }
                }
                if (forbidden.isEmpty()) {
                    continue;
                }
                rules.add(
                        noClasses()
                                .that()
                                .resideInAPackage(pkg(module))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(forbidden.toArray(String[]::new))
                                .allowEmptyShould(true)
                                .because(
                                        "SAD 4.2: "
                                                + module
                                                + " chỉ được gọi module ở tầng thấp hơn"));
            }
        }
        return rules;
    }

    private static List<ArchRule> foreignLayer(String layer) {
        List<ArchRule> rules = new ArrayList<>();
        for (String module : MODULES) {
            rules.add(
                    noClasses()
                            .that()
                            .resideOutsideOfPackage(pkg(module))
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage("vn.edu.tlu.dms." + module + "." + layer + "..")
                            .allowEmptyShould(true)
                            .because(
                                    "SAD 4.2: không dùng "
                                            + layer
                                            + " của module "
                                            + module
                                            + " từ module khác"));
        }
        return rules;
    }
}
