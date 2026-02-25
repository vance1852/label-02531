package com.s3manager.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("通用类测试")
class CommonTest {

    @Nested
    @DisplayName("R 统一响应")
    class RTest {

        @Test
        @DisplayName("ok() - 无数据")
        void ok_noData() {
            R<Void> r = R.ok();
            assertEquals(200, r.getCode());
            assertEquals("success", r.getMessage());
            assertNull(r.getData());
        }

        @Test
        @DisplayName("ok(data) - 有数据")
        void ok_withData() {
            R<String> r = R.ok("hello");
            assertEquals(200, r.getCode());
            assertEquals("hello", r.getData());
        }

        @Test
        @DisplayName("fail(message) - 默认500")
        void fail_defaultCode() {
            R<Void> r = R.fail("error occurred");
            assertEquals(500, r.getCode());
            assertEquals("error occurred", r.getMessage());
            assertNull(r.getData());
        }

        @Test
        @DisplayName("fail(code, message) - 自定义code")
        void fail_customCode() {
            R<Void> r = R.fail(404, "not found");
            assertEquals(404, r.getCode());
            assertEquals("not found", r.getMessage());
        }
    }

    @Nested
    @DisplayName("BizException 业务异常")
    class BizExceptionTest {

        @Test
        @DisplayName("默认code=500")
        void defaultCode() {
            BizException ex = new BizException("something wrong");
            assertEquals(500, ex.getCode());
            assertEquals("something wrong", ex.getMessage());
        }

        @Test
        @DisplayName("自定义code")
        void customCode() {
            BizException ex = new BizException(403, "forbidden");
            assertEquals(403, ex.getCode());
            assertEquals("forbidden", ex.getMessage());
        }
    }
}
