package co.com.pragma.api.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access =  lombok.AccessLevel.PRIVATE)
public class ApiConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Schemas{
        public static final String ERROR_SCHEMA_NAME = "Error Response";
        public static final String ERROR_SCHEMA_DESCRIPTION = "Error Response Details";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiPaths {
        public static final String BASE_PATH = "/api/v1";
        public static final String REPORT_PATH = BASE_PATH + "/reportes";
        public static final String SWAGGER_PATH = "/swagger-ui.html";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiPathMatchers {
        //PERMIT ALL
        public static final String API_DOCS_MATCHER = "/v3/api-docs/**";
        public static final String SWAGGER_UI_MATCHER = "/swagger-ui/**";
        //ADMIN
        public static final String REPORT_MATCHER = ApiPaths.REPORT_PATH +"/**";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Role {
        public static final String CLIENT_ROLE_NAME = "CLIENTE";
        public static final String ADMIN_ROLE_NAME = "ADMIN";
        public static final String ADVISOR_ROLE_NAME = "ASESOR";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ErrorDocs {
        public static final String ERROR_TIMESTAMP_DESCRIPTION = "Timestamp of when the error occurred.";
        public static final String ERROR_PATH_DESCRIPTION = "The path where the error occurred.";
        public static final String ERROR_CODE_DESCRIPTION = "A unique code identifying the error.";
        public static final String ERROR_MESSAGE_DESCRIPTION = "A human-readable message describing the error.";
        public static final String EXAMPLE_ERROR_TIMESTAMP = "2025-01-01T00:00:00.000Z";
        public static final String EXAMPLE_ERROR_CODE = "DOM-001";
        public static final String EXAMPLE_ERROR_MESSAGE = "An error occurred while processing the request.";
    }
}
