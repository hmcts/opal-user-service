package uk.gov.hmcts.opal.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class TokenUtils {
    public static DecodedJWT parseToken(String token) throws JWTDecodeException {
        return JWT.decode(token);
    }
}
