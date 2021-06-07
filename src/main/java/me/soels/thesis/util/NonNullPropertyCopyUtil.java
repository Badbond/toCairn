package me.soels.thesis.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashSet;

/**
 * Utility class to copy fields from one object to another exlcuding {@code null} fields from the source.
 * <p>
 * This is convenient when using patch-operations on a data model.
 * <p>
 * This class is derived from <a href="https://stackoverflow.com/a/19739041">a Stack Overflow answer</a>. The code
 * has been changed to return the target object with the appropriate type.
 */
public class NonNullPropertyCopyUtil {
    private NonNullPropertyCopyUtil() {
        // Utility class, do not instantiate
    }

    /**
     * Copy the non-null properties from {@code source} and apply them to {@code target}.
     *
     * @param src    the source to copy the non-null properties from
     * @param target the target to apply the non-null properties to
     */
    public static void copyProperties(Object src, Object target) {
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }

    private static String[] getNullPropertyNames(Object source) {
        final var src = new BeanWrapperImpl(source);
        var pds = src.getPropertyDescriptors();

        var emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            var srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        var result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}
