package me.soels.tocairn.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom {@link Matcher} for whether a string contains any lower-cased characters.
 */
public class StringContainsLowerCaseMatcher extends TypeSafeMatcher<String> {
    /**
     * Creates a matcher that matches if the examined {@link String} contains any lower-cased character.
     * For example:
     * <pre>assertThat("MY_VALUE", not(containsLowerCasedCharacters()))</pre>
     * <pre>assertThat("myValue", containsLowerCasedCharacters())</pre>
     */
    public static Matcher<String> containsLowerCasedCharacters() {
        return new StringContainsLowerCaseMatcher();
    }

    @Override
    protected boolean matchesSafely(String string) {
        char[] charArray = string.toCharArray();
        for (char c : charArray) {
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string containing any lower-cased characters");
    }
}
