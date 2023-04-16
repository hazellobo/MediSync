package info.neu.infoapp.service;

import com.nimbusds.jose.JOSEException;


public interface AuthorizationService {
    public String generateToken() throws JOSEException;
    public String authorize(String authorization);

}
