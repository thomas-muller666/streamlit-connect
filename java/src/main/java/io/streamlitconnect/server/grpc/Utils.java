package io.streamlitconnect.server.grpc;

import io.streamlitconnect.utils.StringUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.MDC;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
class Utils {

    private static final String KEY_SESSION_ID = "sessionId";
    private static final String KEY_SEQUENCE = "sequence";

    static String randomKeySuffix() {
        return RandomStringUtils.random(6, false, true);
    }

    static void prepareMDC(String sessionId, long seq) {
        MDC.put(KEY_SESSION_ID, StringUtils.truncate(sessionId, 5));
        MDC.put(KEY_SEQUENCE, String.valueOf(seq));
    }

}
