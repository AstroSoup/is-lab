package ru.astrosoup.islab.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.astrosoup.islab.repositories.OrganizationFilter;
import ru.astrosoup.islab.services.OperationsService;
import ru.astrosoup.islab.services.OrganizationService;

import java.util.List;

@Path("organizations")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationApi {
    @Inject
    OrganizationService service;
    @Inject
    OperationsService operations;

    @POST
    public Response create(ApiDtos.OrganizationDto dto) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiDtos.OrganizationDto.of(service.create(dto.toEntity()))).build();
    }

    @GET
    @Path("{id}")
    public ApiDtos.OrganizationDto get(@PathParam("id") long id) {
        return ApiDtos.OrganizationDto.of(service.get(id));
    }

    @GET
    public ApiDtos.OrganizationPage list(@QueryParam("name") String name, @QueryParam("fullName") String fullName,
                                         @QueryParam("offset") @DefaultValue("0") int offset,
                                         @QueryParam("limit") @DefaultValue("100") int limit) {
        OrganizationFilter filter = new OrganizationFilter(name, fullName, null, null, null);
        ApiDtos.OrganizationPage page = new ApiDtos.OrganizationPage();
        page.total = service.count(filter);
        page.items = service.findPage(filter, List.of(), offset, Math.min(limit, 500)).stream()
                .map(ApiDtos.OrganizationDto::of).toList();
        return page;
    }

    @PUT
    @Path("{id}")
    public ApiDtos.OrganizationDto update(@PathParam("id") long id, ApiDtos.OrganizationDto dto) {
        dto.id = id;
        return ApiDtos.OrganizationDto.of(service.update(dto.toEntity()));
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("merge")
    public ApiDtos.OrganizationDto merge(ApiDtos.MergeRequest r) {
        return ApiDtos.OrganizationDto.of(operations.merge(r.firstId, r.secondId, r.name, r.street));
    }

    @POST
    @Path("absorb")
    public ApiDtos.OrganizationDto absorb(ApiDtos.AbsorbRequest r) {
        return ApiDtos.OrganizationDto.of(operations.absorb(r.acquirerId, r.targetId));
    }
}
