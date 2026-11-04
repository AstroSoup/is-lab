package ru.astrosoup.islab.tx;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;

public final class SerializationFailures {
    public static final String MESSAGE =
            "Операция отклонена из-за одновременных изменений других пользователей. Повторите её.";

    private static final int MAX_NODES = 100;

    private SerializationFailures() {
    }


    public static boolean isSerializationFailure(Throwable t) {
        return anyInChain(t, SerializationFailures::isSerializationNode);
    }

    public static boolean isOptimisticLockFailure(Throwable t) {
        return anyInChain(t, c -> {
            String name = c.getClass().getName();
            return name.equals("jakarta.persistence.OptimisticLockException")
                    || name.equals("org.eclipse.persistence.exceptions.OptimisticLockException");
        });
    }

    private static boolean isSerializationNode(Throwable c) {
        if (c instanceof SQLException sql) {
            for (SQLException e = sql; e != null; e = e.getNextException()) {
                if ("40001".equals(e.getSQLState()) || "40P01".equals(e.getSQLState())) {
                    return true;
                }
            }
        }
        String message = c.getMessage();
        return message != null && (message.contains("could not serialize access")
                || message.contains("deadlock detected"));
    }

    private static boolean anyInChain(Throwable t, Predicate<Throwable> test) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> queue = new ArrayDeque<>();
        if (t != null) {
            queue.add(t);
        }
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            Throwable c = queue.poll();
            if (!seen.add(c)) {
                continue;
            }
            if (test.test(c)) {
                return true;
            }
            if (c.getCause() != null) {
                queue.add(c.getCause());
            }
            for (Throwable s : c.getSuppressed()) {
                queue.add(s);
            }
        }
        return false;
    }
}
