package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.ImportOperation;
import ru.astrosoup.islab.entities.User;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.repositories.ImportOperationRepository;
import ru.astrosoup.islab.repositories.UserRepository;

import java.util.List;

@ApplicationScoped
@Transactional
public class ImportHistoryService {
    private static final int MAX_MESSAGE = 4000;

    @Inject
    ImportOperationRepository repository;
    @Inject
    UserRepository users;
    @Inject
    Event<DataChangedEvent> events;

    /** Запись о неуспешной операции — в отдельной транзакции, чтобы откат импорта её не затронул. */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ImportOperation recordFailure(String userName, String fileName, String message) {
        String text = message == null || message.length() <= MAX_MESSAGE
                ? message : message.substring(0, MAX_MESSAGE - 1) + "…";
        ImportOperation operation = ImportOperation.failure(userName, fileName, text);
        repository.persist(operation);
        PersistenceFailures.flush(repository::flush, "Не удалось записать операцию в историю");
        // рассылается после коммита этой отдельной транзакции, поэтому открытые окна истории обновятся сами
        events.fire(new DataChangedEvent(EntityKind.IMPORT_HISTORY));
        return operation;
    }

    /** Видит ли пользователь операции всех пользователей (то есть является ли администратором). */
    public boolean seesAllOperations(String userName) {
        User user = userName == null ? null : users.find(userName);
        return user != null && user.isAdmin();
    }

    public List<ImportOperation> findPage(String userName, int offset, int limit) {
        return seesAllOperations(userName)
                ? repository.findPage(offset, limit)
                : repository.findPageByUser(userName, offset, limit);
    }

    public long count(String userName) {
        return seesAllOperations(userName) ? repository.count() : repository.countByUser(userName);
    }
}
