package co.com.pragma.model.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Errors {

    public static final String INVALID_ENDPOINT_CODE = "IE001";
    public static final String INVALID_ENDPOINT = "Invalid endpoint.";

    public static final String FAIL_READ_REQUEST_CODE = "W001";
    public static final String FAIL_READ_REQUEST = "Failed to read HTTP message.";

    public static final String ACCESS_DENIED_CODE = "AD001";
    public static final String ACCESS_DENIED = "Access denied. You do not have the necessary permissions to access this resource.";

    public static final String INVALID_CREDENTIALS_CODE = "IC001";
    public static final String INVALID_CREDENTIALS = "Invalid credentials.";

    public static final String UNKNOWN_CODE = "UNKNOWN";
    public static final String UNKNOWN_ERROR = "We are sorry, something went wrong. Please try again later.";
}
