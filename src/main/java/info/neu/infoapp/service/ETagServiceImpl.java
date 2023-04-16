package info.neu.infoapp.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class ETagServiceImpl implements ETagService{
    public String generateETag(JSONObject json) {
        String etag = null;
        try {
            MessageDigest msg = MessageDigest.getInstance("SHA-256");
            byte[] hash = msg.digest(json.toString().getBytes(StandardCharsets.UTF_8));
            etag = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return "\"" + etag + "\"";
    }
}
