package me.soels.tocairn.util;

import java.util.List;
import java.util.stream.Collectors;

public class GenericCollectionExtractor {
    private GenericCollectionExtractor() {
        // Utility class, do not instantiate
    }

    /**
     * Returns a subset of the given {@code collection} containing only instances of the given {@code type}.
     *
     * @param collection the collection to retrieve a subset of the expected type from
     * @param type       the type to retrieve a subset of the collection from
     * @param <T>        the type to retrieve a subset of the collection from
     * @return a subset of the given collection containing only classes with the given type
     */
    public static <T> List<T> extractType(List<?> collection, Class<T> type) {
        return collection.stream()
                .filter(clazz -> type.isAssignableFrom(clazz.getClass()))
                .map(type::cast)
                .collect(Collectors.toList());
    }
}
