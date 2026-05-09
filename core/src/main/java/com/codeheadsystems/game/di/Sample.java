package com.codeheadsystems.game.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * Marker for sample-only Dagger bindings. The scaffold uses {@code @BindsOptionalOf @Sample}
 * to declare optional override slots; {@code SampleModule} provides {@code @Sample}-qualified
 * bindings that override the scaffold defaults when wired into the component.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Sample {}
