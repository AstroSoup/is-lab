package ru.astrosoup.islab.tx;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@SerializableIsolation
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class SerializableIsolationInterceptor {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    @AroundInvoke
    public Object apply(InvocationContext context) throws Exception {
        em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE").executeUpdate();
        return context.proceed();
    }
}
