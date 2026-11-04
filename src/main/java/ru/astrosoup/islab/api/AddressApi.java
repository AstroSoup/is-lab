package ru.astrosoup.islab.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.astrosoup.islab.services.AddressService;

import java.util.List;

@Path("addresses")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AddressApi {
    @Inject
    AddressService service;

    @POST
    public Response create(ApiDtos.AddressDto dto) {
        return Response.status(Response.Status.CREATED).entity(ApiDtos.AddressDto.of(service.create(dto.toEntity()))).build();
    }

    @GET
    @Path("{id}")
    public ApiDtos.AddressDto get(@PathParam("id") long id) {
        return ApiDtos.AddressDto.of(service.get(id));
    }

    @GET
    public List<ApiDtos.AddressDto> list(@QueryParam("street") String street) {
        return service.findAll().stream()
                .filter(a -> street == null || street.equals(a.getStreet()))
                .map(ApiDtos.AddressDto::of).toList();
    }

    @PUT
    @Path("{id}")
    public ApiDtos.AddressDto update(@PathParam("id") long id, ApiDtos.AddressDto dto) {
        dto.id = id;
        return ApiDtos.AddressDto.of(service.update(dto.toEntity()));
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
