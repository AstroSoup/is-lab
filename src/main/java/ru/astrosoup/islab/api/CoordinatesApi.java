package ru.astrosoup.islab.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.astrosoup.islab.services.CoordinatesService;

import java.util.List;

@Path("coordinates")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CoordinatesApi {
    @Inject
    CoordinatesService service;

    @POST
    public Response create(ApiDtos.CoordinatesDto dto) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiDtos.CoordinatesDto.of(service.create(dto.toEntity()))).build();
    }

    @GET
    @Path("{id}")
    public ApiDtos.CoordinatesDto get(@PathParam("id") long id) {
        return ApiDtos.CoordinatesDto.of(service.get(id));
    }

    @GET
    public List<ApiDtos.CoordinatesDto> list(@QueryParam("x") Double x, @QueryParam("y") Integer y) {
        return service.findAll().stream()
                .filter(c -> x == null || Double.compare(c.getX(), x) == 0)
                .filter(c -> y == null || c.getY() == y)
                .map(ApiDtos.CoordinatesDto::of).toList();
    }

    @PUT
    @Path("{id}")
    public ApiDtos.CoordinatesDto update(@PathParam("id") long id, ApiDtos.CoordinatesDto dto) {
        dto.id = id;
        return ApiDtos.CoordinatesDto.of(service.update(dto.toEntity()));
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
