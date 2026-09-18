package com.lifehub.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the current authenticated user's id into a controller method parameter of type {@link Long}.
 * Resolved from {@link AppUserPrincipal} in the SecurityContext, regardless of how authentication
 * is actually performed (seed user today, real login later).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
