package com.ggasoftware.uitest.control.base.asserter;

import com.ggasoftware.uitest.utils.linqInterfaces.JActionT;
import com.ggasoftware.uitest.utils.linqInterfaces.JFuncT;

import java.util.Collection;

import static com.ggasoftware.uitest.control.base.asserter.DoScreen.*;
import static com.ggasoftware.uitest.control.base.logger.enums.LogInfoTypes.FRAMEWORK;
import static com.ggasoftware.uitest.control.base.usefulUtils.ScreenshotMaker.doScreenshotGetMessage;
import static com.ggasoftware.uitest.utils.LinqUtils.first;
import static com.ggasoftware.uitest.utils.LinqUtils.select;
import static com.ggasoftware.uitest.utils.PrintUtils.print;
import static com.ggasoftware.uitest.utils.PrintUtils.printObjectAsArray;
import static com.ggasoftware.uitest.utils.ReflectionUtils.isInterface;
import static com.ggasoftware.uitest.utils.TestBaseWebDriver.logger;
import static java.lang.String.format;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.Arrays.asList;

/**
 * Created by Roman_Iovlev on 6/9/2015.
 */
public abstract class BaseChecker implements IAsserter, IChecker {
    public static DoScreen defaultDoScreenType = NO_SCREEN;
    private JActionT<String> throwFail;
    private DoScreen doScreenshot = defaultDoScreenType;
    private String checkMessage = "";
    private boolean ignoreCase = false;
    private boolean isListCheck = false;

    public BaseChecker() {
    }

    public BaseChecker(String checkMessage) {
        this.checkMessage = getCheckMessage(checkMessage);
    }

    public BaseChecker doScreenshot(DoScreen doScreenshot) {
        this.doScreenshot = doScreenshot;
        return this;
    }

    public BaseChecker doScreenshot() {
        return doScreenshot(DO_SCREEN_ALWAYS);
    }

    public BaseChecker setThrowFail(JActionT<String> throwFail) {
        this.throwFail = throwFail;
        return this;
    }

    public BaseChecker ignoreCase() {
        this.ignoreCase = true;
        return this;
    }

    private String getCheckMessage(String checkMessage) {
        if (checkMessage == null || checkMessage.equals("")) return "";
        String firstWord = checkMessage.split(" ")[0];
        return (!firstWord.toLowerCase().equals("check") || firstWord.toLowerCase().equals("verify"))
                ? "Check " + checkMessage
                : checkMessage;
    }

    private void assertAction(String defaultMessage, Boolean result, String failMessage) {
        assertAction(defaultMessage, () -> result ? null : "Check failed", failMessage);
    }

    private void assertAction(String defaultMessage, JFuncT<String> result, String failMessage) {
        if (!isListCheck && defaultMessage != null)
            logger.info(getBeforeMessage(defaultMessage));
        if (!isListCheck && doScreenshot == DO_SCREEN_ALWAYS)
            makeScreenshot();
        if (isListCheck && failMessage == null)
            failMessage = defaultMessage + " failed";
        String resultMessage = result.invoke();
        if (resultMessage != null) {
            if (doScreenshot == SCREEN_ON_FAIL)
                makeScreenshot();
            throwFail.invoke(failMessage != null
                    ? failMessage
                    : resultMessage);
        }
    }

    private void makeScreenshot() {
        String screenMessage = doScreenshotGetMessage();
        logger.info("Create screenshot in: ", screenMessage);
    }

    private String getBeforeMessage(String defaultMessage) {
        return (checkMessage != null && !checkMessage.equals(""))
                ? checkMessage
                : defaultMessage;
    }

    // For Framework
    public RuntimeException exception(String failMessage) {
        logger.error(FRAMEWORK, failMessage);
        assertAction(null, false, failMessage);
        return new RuntimeException(failMessage);
    }

    // Asserts
    public void areEquals(Object actual, Object expected, String failMessage) {
        if (ignoreCase && actual.getClass() == String.class) {
            actual = ((String) actual).toLowerCase();
            expected = ((String) expected).toLowerCase();
        }
        assertAction(format("Check that '%s' equals to '%s'", actual, expected), actual.equals(expected), failMessage);
    }

    public void areEquals(Object actual, Object expected) {
        areEquals(actual, expected, null);
    }

    public void matches(String actual, String regEx, String failMessage) {
        if (ignoreCase) {
            actual = actual.toLowerCase();
            regEx = regEx.toLowerCase();
        }
        assertAction(format("Check that '%s' matches to regEx '%s", actual, regEx), actual.matches(regEx), failMessage);
    }

    public void matches(String actual, String regEx) {
        matches(actual, regEx, null);
    }

    public void contains(String actual, String expected, String failMessage) {
        if (ignoreCase) {
            actual = actual.toLowerCase();
            expected = expected.toLowerCase();
        }
        assertAction(format("Check that '%s' contains '%s'", actual, expected), actual.contains(expected), failMessage);
    }

    public void contains(String actual, String expected) {
        contains(actual, expected, null);
    }

    public void isTrue(Boolean condition, String failMessage) {
        assertAction(format("Check that condition '%s' is True", condition), condition, failMessage);
    }

    public void isTrue(Boolean condition) {
        isTrue(condition, null);
    }

    public void isFalse(Boolean condition, String failMessage) {
        assertAction(format("Check that condition '%s' is False", condition), !condition, failMessage);
    }

    public void isFalse(Boolean condition) {
        isFalse(condition, null);
    }

    private boolean isObjEmpty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String)
            return obj.toString().equals("");
        if (isInterface(obj.getClass(), Collection.class))
            return ((Collection) obj).size() == 0;
        if (obj.getClass().isArray())
            return getLength(obj) == 0;
        return false;
    }

    public void isEmpty(Object obj, String failMessage) {
        assertAction("Check that Object is empty", isObjEmpty(obj), failMessage);
    }

    public void isEmpty(Object obj) {
        isEmpty(obj, null);
    }

    public void isNotEmpty(Object obj, String failMessage) {
        assertAction("Check that Object is NOT empty", !isObjEmpty(obj), failMessage);
    }

    public void isNotEmpty(Object obj) {
        isNotEmpty(obj, null);
    }

    public void areSame(Object actual, Object expected, String failMessage) {
        assertAction("Check that Objects are the same", actual == expected, failMessage);
    }

    public void areSame(Object actual, Object expected) {
        areSame(actual, expected, null);
    }

    public void areDifferent(Object actual, Object expected, String failMessage) {
        assertAction("Check that Objects are different", actual != expected, failMessage);
    }

    public void areDifferent(Object actual, Object expected) {
        areDifferent(actual, expected, null);
    }

    public <T> void listEquals(Collection<T> collection, Collection<T> collection2, String failMessage) {
        assertAction("Check that Collections are equal",
                () -> collection != null && collection2 != null && collection.size() == collection2.size()
                        ? null
                        : "listEquals failed because one of the Collections is null or empty",
                failMessage);
        assertAction(null, () -> {
            T notEqualElement = first(collection, el -> !collection2.contains(el));
            return (notEqualElement != null)
                    ? format("Collections '%s' and '%s' not equals at element '%s'",
                    print(select(collection, Object::toString)), print(select(collection2, Object::toString)), notEqualElement)
                    : null;
        }, failMessage);
    }

    public <T> void listEquals(Collection<T> collection, Collection<T> collection2) {
        listEquals(collection, collection2, null);
    }

    public <T> void arrayEquals(T array, T array2, String failMessage) {
        assertAction("Check that Collections are equal",
                () -> array != null && array2 != null && array.getClass().isArray() && array2.getClass().isArray()
                        && getLength(array) == getLength(array2)
                        ? null
                        : "arrayEquals failed because one of the Objects is not Array or empty",
                failMessage);
        assertAction(null, () -> {
            for (int i = 0; i <= getLength(array); i++)
                if (!get(array, i).equals(get(array2, i)))
                    return format("Arrays not equals at index '%s'. '%s' != '%s'. Arrays: '%s' and '%s'",
                            i, get(array, i), get(array2, i), printObjectAsArray(array), printObjectAsArray(array2));
            return null;
        }, failMessage);
    }

    public <T> void arrayEquals(T array, T array2) {
        arrayEquals(array, array2, null);
    }

    // ListProcessor
    public <T> ListChecker eachElementOf(Collection<T> list) {
        return new ListChecker<>(list);
    }

    public <T> ListChecker eachElementOf(T[] array) {
        return new ListChecker<>(asList(array));
    }

    public class ListChecker<T> {
        Collection<T> list;

        private ListChecker(Collection<T> list) {
            this.list = list;
        }

        private void beforeListCheck(String defaultMessage, String expected, String failMessage) {
            assertAction(format(defaultMessage, print(select(list, Object::toString)), expected),
                    () -> list != null && list.size() > 0
                            ? null
                            : "list check failed because list is null or empty",
                    failMessage);
            isListCheck = true;
        }

        public void areEquals(Object expected, String failMessage) {
            beforeListCheck("Check that each item of list '%s' equals to '%s'", expected.toString(), failMessage);
            for (Object el : list)
                BaseChecker.this.areEquals(el, expected, failMessage);
        }

        public void areEquals(Object expected) {
            areEquals(expected, null);
        }

        public void matches(String regEx, String failMessage) {
            beforeListCheck("Check that each item of list '%s' matches to regEx '%s'", regEx, failMessage);
            for (Object el : list)
                BaseChecker.this.matches((String) el, regEx, failMessage);
        }

        public void matches(String regEx) {
            matches(regEx, null);
        }

        public void contains(String expected, String failMessage) {
            beforeListCheck("Check that each item of list '%s' contains '%s'", expected, failMessage);
            for (Object el : list)
                BaseChecker.this.contains((String) el, expected, failMessage);
        }

        public void contains(String expected) {
            contains(expected, null);
        }

        public void areSame(Object expected, String failMessage) {
            beforeListCheck("Check that all items of list '%s' are same with '%s'", expected.toString(), failMessage);
            for (Object el : list)
                BaseChecker.this.areSame(el, expected, failMessage);
        }

        public void areSame(Object actual, Object expected) {
            areSame(expected, null);
        }

        public void areDifferent(Object expected, String failMessage) {
            beforeListCheck("Check that all items of list '%s' are different with '%s'", expected.toString(), failMessage);
            for (Object el : list)
                BaseChecker.this.areDifferent(el, expected, failMessage);
        }

        public void areDifferent(Object actual, Object expected) {
            areDifferent(expected, null);
        }
    }

}
