package me.soels.thesis.util;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;

/**
 * Custom {@link Matcher} for whether a string contains a string ignoring any casing and allowing for matching
 * the string on a {@code null} input substring.
 * <p>
 * When the input substring was {@code null}, this matcher will always return {@code false}. This is a convenience
 * function to be able to match on injected configuration strings (which can be unconfigured; {@code null}).
 */
public class StringContainsIgnoreCaseMatcher extends StringContains {
    private final boolean matchingStringWasNull;

    private StringContainsIgnoreCaseMatcher(String substring, boolean matchingStringWasNull) {
        super(true, substring);
        this.matchingStringWasNull = matchingStringWasNull;
    }

    /**
     * Creates a matcher that matches if the examined {@link String} contains the specified
     * {@link String} anywhere, ignoring case. This matcher furthermore allows the substring to be {@code null},
     * always resulting in {@code false}.
     * For example:
     * <pre>assertThat("myStringOfNote", containsStringIgnoringCase("Ring"))</pre>
     *
     * @param substring the substring that the returned matcher will expect to find within any examined string
     */
    public static Matcher<String> containsStringIgnoringCase(String substring) {
        var value = substring == null ? "" : substring;
        return new StringContainsIgnoreCaseMatcher(value, substring == null);
    }

    @Override
    protected boolean evalSubstringOf(String s) {
        if (matchingStringWasNull) {
            return false;
        }
        return super.evalSubstringOf(s);
    }
}
