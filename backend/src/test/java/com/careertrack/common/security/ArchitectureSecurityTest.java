package com.careertrack.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureSecurityTest {

    private static final Set<String> PUBLIC_WHITELIST_METHODS = Set.of(
            "login",
            "refreshToken"
    );

    @Test
    @DisplayName("Chaque endpoint REST doit posséder une annotation @PreAuthorize non vide")
    void allControllerEndpointsMustHavePreAuthorize() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> violations = new ArrayList<>();

        for (var beanDef : scanner.findCandidateComponents("com.careertrack")) {
            Class<?> clazz = Class.forName(beanDef.getBeanClassName());
            PreAuthorize classPreAuthorize = clazz.getAnnotation(PreAuthorize.class);

            for (Method method : clazz.getDeclaredMethods()) {
                if (isEndpointMethod(method)) {
                    PreAuthorize methodPreAuthorize = method.getAnnotation(PreAuthorize.class);

                    // Si pas sur la méthode ni sur la classe
                    if (methodPreAuthorize == null && classPreAuthorize == null) {
                        if (!PUBLIC_WHITELIST_METHODS.contains(method.getName())) {
                            violations.add(String.format("MANQUE @PreAuthorize: %s#%s()", clazz.getSimpleName(), method.getName()));
                        }
                    } else {
                        // Vérifier qu'il n'y a pas d'usage abusif de permitAll() hors liste blanche
                        String spEl = methodPreAuthorize != null ? methodPreAuthorize.value() : classPreAuthorize.value();
                        if (spEl.contains("permitAll") && !PUBLIC_WHITELIST_METHODS.contains(method.getName())) {
                            violations.add(String.format("INTERDIT @PreAuthorize('permitAll()') HORS LISTE BLANCHE: %s#%s()",
                                    clazz.getSimpleName(), method.getName()));
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .withFailMessage("Des endpoints ne respectent pas la politique de sécurité :\n%s", String.join("\n", violations))
                .isEmpty();
    }

    private boolean isEndpointMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
