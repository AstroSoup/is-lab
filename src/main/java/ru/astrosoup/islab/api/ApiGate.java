package ru.astrosoup.islab.api;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import ru.astrosoup.islab.services.AuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiGate implements ContainerRequestFilter {
    public static final String USER = "islab.user";

    @Inject
    AuthService auth;

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!Boolean.getBoolean("islab.api.enabled")) {
            ctx.abortWith(Response.status(Response.Status.NOT_FOUND).build());
            return;
        }
        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.equals("register") || path.equals("register/")) {
            return;
        }
        String[] credentials = parseBasic(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (credentials == null || !auth.authenticate(credentials[0], credentials[1])) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"is-lab\"").build());
            return;
        }
        String login = credentials[0].trim();
        ctx.setProperty(USER, login);
        boolean secure = ctx.getSecurityContext() != null && ctx.getSecurityContext().isSecure();
        ctx.setSecurityContext(new SecurityContext() {
            @Override
            public java.security.Principal getUserPrincipal() {
                return () -> login;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public String getAuthenticationScheme() {
                return BASIC_AUTH;
            }
        });
    }

    private static String[] parseBasic(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            return colon < 0 ? null : new String[]{decoded.substring(0, colon), decoded.substring(colon + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
