package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeneralResponseTest {
    @Test
    public void commentUnavailableIncludesDeletedAndNotThis() {
        assertTrue(GeneralResponse.isCommentUnavailable(GeneralResponse.CODE_COMMENT_DELETED));
        assertTrue(GeneralResponse.isCommentUnavailable(GeneralResponse.CODE_COMMENT_NOT_THIS));
    }

    @Test
    public void unrelatedCodesAreNotCommentUnavailable() {
        assertFalse(GeneralResponse.isCommentUnavailable(GeneralResponse.CODE_SUCCESS));
        assertFalse(GeneralResponse.isCommentUnavailable(GeneralResponse.CODE_COMMENT_CONTAIN_SENSITIVE));
    }
}
