package info.neu.infoapp.service;

import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.stream.Collectors;


public interface PlanService {
    public List getAllPlans();
    @SneakyThrows
    public String savePlan(JSONObject planObject, String planKey) throws JSONException;
    public void delete(String id) ;
    public Map<String, Object> getPlanById(String keyId);
    public boolean ifKeyExists(String objectKey);
    public String getETag(String eTagKey);
    public String updateETag(String eTagKey, JSONObject eTagValue);


}
