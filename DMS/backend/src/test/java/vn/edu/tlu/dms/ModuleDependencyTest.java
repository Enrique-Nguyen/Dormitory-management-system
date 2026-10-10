package vn.edu.tlu.dms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm tra quy tắc phụ thuộc giữa các module (SAD mục 4.2). Test đỏ nghĩa là có lời gọi sai chiều:
 * chuyển sang dùng sự kiện, hoặc gọi Service công khai của module tầng thấp hơn.
 */
@AnalyzeClasses(packages = "vn.edu.tlu.dms", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleDependencyTest {

    /** Thứ tự tầng từ thấp lên. Module chỉ được gọi module ở tầng thấp hơn; cùng tầng không gọi nhau. */
    private static final List<List<String>> TIERS = List.of(
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
                rules.add(noClasses().that().resideInAPackage(pkg(module))
                    .should().dependOnClassesThat().resideInAnyPackage(forbidden.toArray(String[]::new))
                    .allowEmptyShould(true)
                    .because("SAD 4.2: " + module + " chỉ được gọi module ở tầng thấp hơn"));
            }
        }
        return rules;
    }

    private static List<ArchRule> foreignLayer(String layer) {
        List<ArchRule> rules = new ArrayList<>();
        for (String module : MODULES) {
            rules.add(noClasses().that().resideOutsideOfPackage(pkg(module))
                .should().dependOnClassesThat().resideInAPackage("vn.edu.tlu.dms." + module + "." + layer + "..")
                .allowEmptyShould(true)
                .because("SAD 4.2: không dùng " + layer + " của module " + module + " từ module khác"));
        }
        return rules;
    }
}
