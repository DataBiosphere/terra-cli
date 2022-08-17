package harness;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that a class should only be run locally, and not on a remote runner.
 * This is primarily useful for tests which require the CLI docker image or local tools to be
 * installed, as locally-built images are not available on remote runners.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LocalTestOnly {}
