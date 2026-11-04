package ru.astrosoup.islab.tx;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import ru.astrosoup.islab.exceptions.ConflictException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


@SerializableIsolation
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 150)
public class SerializableRetryInterceptor {
    static final int MAX_ATTEMPTS = 8;
    private static final Logger LOG = Logger.getLogger(SerializableRetryInterceptor.class.getName());

    @AroundInvoke
    public Object retry(InvocationContext context) throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                return context.proceed();
            } catch (Exception e) {
                if (!SerializationFailures.isSerializationFailure(e)) {
                    throw e;
                }
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ConflictException(SerializationFailures.MESSAGE, e);
                }
                LOG.fine(() -> context.getMethod().getName() + ": сбой сериализации, повтор");
                // случайная пауза, чтобы конфликтующие транзакции не повторялись синхронно
                Thread.sleep(ThreadLocalRandom.current().nextLong(5L * attempt, 25L * attempt + 1));
            }
        }
    }
}
