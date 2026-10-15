package ru.astrosoup.islab.events;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Рассылает всем подписчикам (открытым окнам пользователей) уведомления об изменениях.
 * Срабатывает только ПОСЛЕ успешного коммита транзакции, поэтому клиенты всегда читают уже сохранённые данные.
 */
@ApplicationScoped
public class ChangeBroadcaster {
    private static final Logger LOG = Logger.getLogger(ChangeBroadcaster.class.getName());

    @Resource
    private ManagedExecutorService executor;

    private final List<Consumer<DataChangedEvent>> listeners = new CopyOnWriteArrayList<>();

    /** @return действие, отменяющее подписку */
    public Runnable register(Consumer<DataChangedEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    void onChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) DataChangedEvent event) {
        for (Consumer<DataChangedEvent> listener : listeners) {
            executor.execute(() -> {
                try {
                    listener.accept(event);
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "Listener failed", e);
                }
            });
        }
    }
}
