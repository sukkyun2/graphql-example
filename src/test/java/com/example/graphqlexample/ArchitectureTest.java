package com.example.graphqlexample;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    ApplicationModules modules = ApplicationModules.of(GraphqlExampleApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
