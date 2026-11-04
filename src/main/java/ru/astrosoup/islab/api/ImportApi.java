package ru.astrosoup.islab.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.astrosoup.islab.services.ImportHistoryService;
import ru.astrosoup.islab.services.ImportResult;
import ru.astrosoup.islab.services.ImportService;

import java.util.List;

@Path("import")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImportApi {
    @Inject
    ImportService importService;
    @Inject
    ImportHistoryService history;


    private static String user(SecurityContext security) {
        return security.getUserPrincipal().getName();
    }

    @POST
    @Consumes({"text/csv", MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM})
    public Response importCsv(byte[] content, @QueryParam("fileName") @DefaultValue("api.csv") String fileName,
                              @Context SecurityContext security) {
        ImportResult result = importService.importOrganizations(user(security), fileName, content);
        ApiDtos.ImportResultDto dto = new ApiDtos.ImportResultDto();
        dto.success = result.success();
        dto.operationId = result.operation() == null ? null : result.operation().getId();
        dto.added = result.success() ? result.operation().getAddedCount() : null;
        dto.errors = result.errors();
        return Response.status(result.success() ? 200 : 422).entity(dto).build();
    }

    @GET
    @Path("history")
    public List<ApiDtos.HistoryItem> history(@QueryParam("offset") @DefaultValue("0") int offset,
                                             @QueryParam("limit") @DefaultValue("200") int limit,
                                             @Context SecurityContext security) {
        return history.findPage(user(security), offset, Math.min(limit, 1000)).stream().map(op -> {
            ApiDtos.HistoryItem item = new ApiDtos.HistoryItem();
            item.id = op.getId();
            item.status = op.getStatus().name();
            item.user = op.getUserName();
            item.added = op.getAddedCount();
            return item;
        }).toList();
    }
}
