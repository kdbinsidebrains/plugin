package org.kdb.inside.brains;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.RunsInEdt;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@RunsInEdt
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnforceWriteAction.Extension.class)
public @interface EnforceWriteAction {
    class Extension implements InvocationInterceptor {
        @Override
        public void interceptTestMethod(Invocation<Void> invocation,
                                        ReflectiveInvocationContext<Method> invocationContext,
                                        ExtensionContext extensionContext) throws Throwable {
            WriteAction.runAndWait(invocation::proceed);
        }
    }
}
