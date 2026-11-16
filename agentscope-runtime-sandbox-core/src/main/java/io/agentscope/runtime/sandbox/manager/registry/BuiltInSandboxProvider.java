/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.sandbox.manager.registry;

import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.box.BFCLSandbox;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.box.GuiSandbox;
import io.agentscope.runtime.sandbox.box.MobileSandbox;
import io.agentscope.runtime.sandbox.box.WebShopSandbox;
import java.util.Collection;
import java.util.List;

public class BuiltInSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return List.of(
                BaseSandbox.class,
                FilesystemSandbox.class,
                BrowserSandbox.class,
                APPWorldSandbox.class,
                BFCLSandbox.class,
                GuiSandbox.class,
                MobileSandbox.class,
                WebShopSandbox.class);
    }
}
