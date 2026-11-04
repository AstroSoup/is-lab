package ru.astrosoup.islab.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.exceptions.ConflictException;
import ru.astrosoup.islab.exceptions.EntityValidationException;
import ru.astrosoup.islab.exceptions.ObjectNotFoundException;
import ru.astrosoup.islab.tx.SerializationFailures;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {
        if (t instanceof WebApplicationException web) {
            return web.getResponse();
        }
        BusinessException business = findBusiness(t);
        ApiDtos.ErrorDto body = new ApiDtos.ErrorDto();
        int status;
        if (business != null) {
            body.error = business.getMessage();
            if (business instanceof EntityValidationException validation) {
                body.violations = validation.getViolations();
                status = 422;
            } else if (business instanceof ObjectNotFoundException) {
                status = 404;
            } else if (business instanceof ConflictException) {
                status = 409;
            } else {
                status = 400;
            }
        } else if (SerializationFailures.isSerializationFailure(t) || SerializationFailures.isOptimisticLockFailure(t)) {
            // конфликт, обнаруженный уже при COMMIT (после возврата из сервиса), — это 409, а не сбой сервера
            body.error = "Данные были изменены другим пользователем. Повторите операцию.";
            body.violations = List.of();
            status = 409;
        } else {
            LOG.log(Level.SEVERE, "Непредвиденная ошибка REST-запроса", t);
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            body.error = String.valueOf(root.getMessage());
            body.violations = List.of();
            status = 500;
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private static BusinessException findBusiness(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof BusinessException b) {
                return b;
            }
        }
        return null;
    }
}
