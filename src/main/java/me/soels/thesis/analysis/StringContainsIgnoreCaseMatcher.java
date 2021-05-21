package me.soels.thesis.analysis;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;

/**
 * Custom {@link Matcher} for whether a string contains a string ignoring any casing.
 */
public class StringContainsIgnoreCaseMatcher extends SubstringMatcher {
    public StringContainsIgnoreCaseMatcher(String substring) {
        super(substring);
    }

    @Override
    protected String relationship() {
        return "containing (ignoring case)";
    }

    @Override
    protected boolean evalSubstringOf(String string) {
        return StringUtils.containsIgnoreCase(string, super.substring);
    }


    /**
     * Creates a matcher that matches if the examined String contains the specified String anywhere in any casing.
     * <p>
     * For example:
     * assertThat("myStringOfNote", containsIgnoringCase("note"))
     * <p>
     * {@code substring} can be {@code null} resulting in that it will never match any string.
     *
     * @param substring – the substring that the returned matcher will expect to find within any examined string
     * @return the matcher
     */
    @Factory
    public static Matcher<String> containsIgnoringCase(String substring) {
        return new StringContainsIgnoreCaseMatcher(substring);
    }
}
