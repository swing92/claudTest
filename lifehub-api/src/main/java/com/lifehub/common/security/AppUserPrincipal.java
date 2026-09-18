package com.lifehub.common.security;

/**
 * Authenticated principal carrying only the current user's id.
 * Domain code should depend on this type (via {@code @CurrentUserId}), never on the User entity directly.
 */
public record AppUserPrincipal(Long userId) {
}
