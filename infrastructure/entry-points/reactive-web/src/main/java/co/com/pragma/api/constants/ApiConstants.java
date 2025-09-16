package co.com.pragma.api.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access =  lombok.AccessLevel.PRIVATE)
public class ApiConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiPaths {
        public static final String BASE_PATH = "/api/v1";
        public static final String REPORT_PATH = BASE_PATH + "/reportes";
    }
}
