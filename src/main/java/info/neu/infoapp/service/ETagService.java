package info.neu.infoapp.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public interface ETagService {
    public String generateETag(JSONObject json);
}
