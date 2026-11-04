package ru.astrosoup.islab.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.astrosoup.islab.services.AuthService;

@Path("register")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
public class AuthApi {
    @Inject
    AuthService auth;

    @POST
    public Response register(ApiDtos.Credentials credentials) {
        auth.register(credentials.name, credentials.password);
        return Response.status(Response.Status.CREATED).build();
    }
}
