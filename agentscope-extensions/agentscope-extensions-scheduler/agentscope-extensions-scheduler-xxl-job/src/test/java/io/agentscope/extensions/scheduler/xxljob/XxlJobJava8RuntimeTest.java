/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.extensions.scheduler.xxljob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import groovy.lang.GroovyShell;
import org.junit.jupiter.api.Test;

/**
 * Exercises the executor's Groovy dependency on the actual target JVM.
 *
 * @author 芋道源码
 */
class XxlJobJava8RuntimeTest {

    @Test
    void groovySelectsACompatibleRuntimeAndExecutesClosures() throws Exception {
        GroovyShell shell = new GroovyShell();
        try {
            assertEquals(42, shell.evaluate("[10, 11].collect { it * 2 }.sum()"));
        } finally {
            shell.getClassLoader().close();
        }
    }
}
