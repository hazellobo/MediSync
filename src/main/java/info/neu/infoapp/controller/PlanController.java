package info.neu.infoapp.controller;

import info.neu.infoapp.config.RabbitMQConfig;
import info.neu.infoapp.model.Plan;
import info.neu.infoapp.model.Result;
import info.neu.infoapp.service.AuthorizationService;
import info.neu.infoapp.service.PlanService;
import info.neu.infoapp.service.JsonValidatorService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    private final JsonValidatorService jsonValidator;

    private final AuthorizationService authorizationService;

    private final RabbitTemplate template;



    boolean hasAccess(@RequestHeader HttpHeaders requestHeaders){
        boolean hasAccess=false;
        String result = authorizationService.authorize(requestHeaders.getFirst("Authorization"));
        if (result.equals("VALID TOKEN")) {
           hasAccess=true;
        }
        return hasAccess;
    }
    @PostMapping(value = "/addPlan")
    ResponseEntity addPlan(@RequestBody String request, @RequestHeader HttpHeaders requestHeaders) {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            JSONObject jsonObj = new JSONObject(request);
            if (jsonObj.isEmpty() || !jsonValidator.validateJSONSchema(jsonObj)) {
                Result r = new Result("Payload validation failed",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }
            String objectKey = jsonObj.get("objectType") + ":" + jsonObj.get("objectId");
            boolean ifExistingPlan = planService.ifKeyExists(objectKey);
            if (!ifExistingPlan) {
                JSONObject obj = new JSONObject();
                obj.put("ObjectId", jsonObj.get("objectId"));

                Result r = new Result("Object Added",  HttpStatus.CREATED.value(), jsonObj.get("objectId"));

                Map<String, String> actionMap = new HashMap<>();
                actionMap.put("operation", "SAVE");
                actionMap.put("body", request);
                System.out.println("Sending message: " + actionMap);
                template.convertAndSend(RabbitMQConfig.queueName, actionMap);

                String eTagGeneratedAfterSave = planService.savePlan(jsonObj, objectKey);
                return ResponseEntity.created(new URI("/plan/" + objectKey)).eTag(eTagGeneratedAfterSave)
                        .body(r);
            } else {
                Result r = new Result("Object already added",  HttpStatus.CONFLICT.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.CONFLICT);
            }
        } catch (Exception e) {
            Result r = new Result("BAD_REQUEST",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(value = "/{objectType}/{objectId}",produces ={})
    public ResponseEntity getPlan(@PathVariable String objectType, @PathVariable String objectId, @RequestHeader HttpHeaders requestHeaders)  {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            String key = objectType + ":" + objectId;
            Map<String, Object> plan = planService.getPlanById(key);
            if (plan == null || plan.isEmpty()) {
                return new ResponseEntity(new Result("Object does not exists!",  HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>()), HttpStatus.NOT_FOUND);
            } else {
                String ifNoneMatchHeader;
                String ifMatchHeader;
                try {
                    ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
                    ifMatchHeader = requestHeaders.getFirst("If-Match");
                } catch (Exception e) {
                    Result r = new Result("Invalid E-Tag", 200, new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
                }
                String etagFromCache = planService.getETag(key);
                if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                    Result r = new Result("Pre Condition Failed", HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>( HttpStatus.PRECONDITION_FAILED);
                }

                if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                    Result r = new Result("", HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
                }


                if (objectType.equalsIgnoreCase("plan")) {
                    Result r = new Result("Success",  HttpStatus.OK.value(), plan);
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("ETag", etagFromCache);
                    httpHeaders.add("Accept","application/json");
                    httpHeaders.add("Content-Type","application/json");
                    return new ResponseEntity<>(new JSONObject(plan).toString(),httpHeaders, HttpStatus.OK);
                } else {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("ETag", etagFromCache);
                    httpHeaders.add("Accept","application/json");
                    httpHeaders.add("Content-Type","application/json");
                    return new ResponseEntity<>(new JSONObject(plan).toString(),httpHeaders, HttpStatus.OK);
                }
            }
        } catch (Exception e) {
            Result r = new Result(e.getMessage(), HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
    }



    @DeleteMapping(value = "/{objectType}/{objectId}")
    ResponseEntity deletePlan(@PathVariable String objectId, @PathVariable String objectType, @RequestHeader HttpHeaders requestHeaders) {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            String key = objectType + ":" + objectId;
            boolean isExistingPlan = planService.ifKeyExists(key);
            if (!isExistingPlan) {
                Result r = new Result("Object does not exists",  HttpStatus.NOT_FOUND.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
            } else {
                String ifNoneMatchHeader;
                String ifMatchHeader;
                try {
                    ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
                    ifMatchHeader = requestHeaders.getFirst("If-Match");
                } catch (Exception e) {
                    Result r = new Result("Invalid E-Tag Value",  HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
                }
                String etagFromCache = planService.getETag(key);
                if(ifMatchHeader.isEmpty() ){
                    Result r = new Result("E-Tag Missing", HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
                }
                if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                    Result r = new Result("Pre Condition Failed",  HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.PRECONDITION_FAILED);
                }

                if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                    Result r = new Result("",  HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.NOT_MODIFIED);
                }
                Map<String, Object> plan = planService.getPlanById(key);

                Map<String, String> actionMap = new HashMap<>();
                actionMap.put("operation", "DELETE");
                actionMap.put("body", new JSONObject(plan).toString());
                System.out.println("Sending message: " + actionMap);
                template.convertAndSend(RabbitMQConfig.queueName, actionMap);

                planService.delete(key);
                Result r = new Result("Object deleted", HttpStatus.NO_CONTENT.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.NO_CONTENT);
            }
        } catch (Exception e) {
            Result r = new Result("", HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{objectType}/{objectId}")
    ResponseEntity patch(@PathVariable String objectId, @PathVariable String objectType, @RequestBody String jsonData, @RequestHeader HttpHeaders requestHeaders) {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            if (jsonData == null || jsonData.isEmpty()) {

                Result r = new Result("Request body is Empty",  HttpStatus.BAD_REQUEST.value(), "");
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }
            JSONObject jsonPlan = new JSONObject(jsonData);

            if (!jsonValidator.validateJSONSchema(jsonPlan)) {
                Result r = new Result("Payload validation failed", HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }

            String key = objectType + ":" + objectId;

            if (!planService.ifKeyExists(key)) {
                Result r = new Result("Object does not exists", HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
            }

            String etagFromCache = planService.getETag(key);
            String ifMatchHeader = requestHeaders.getFirst("If-Match");
            String ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");

            if(ifMatchHeader.isEmpty()){
                Result r = new Result("E-Tag Missing", HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }
            if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                Result r = new Result("Pre Condition Failed", HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.PRECONDITION_FAILED);
            }

            if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                Result r = new Result("",  HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.NOT_MODIFIED);
            }

            String newEtag = planService.savePlan(jsonPlan, key);

            Map<String, Object> plan = this.planService.getPlanById(key);
            Map<String, String> actionMap = new HashMap<>();
            actionMap.put("operation", "SAVE");
            actionMap.put("body", new JSONObject(plan).toString());
            template.convertAndSend(RabbitMQConfig.queueName, actionMap);

           return ResponseEntity.created(new URI("/" + objectType + "/" + objectId)).eTag(newEtag).body(plan);

        } catch (RuntimeException e) {
            Result r = new Result("Error", HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        } catch (URISyntaxException e) {
            Result r = new Result("Invalid request",HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        } catch (FileNotFoundException e) {
            Result r = new Result("File missing for JSON Validation",HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
    }


    @PutMapping ("/{objectType}/{objectId}")
    ResponseEntity update(@PathVariable String objectId, @PathVariable String objectType, @RequestBody String jsonData, @RequestHeader HttpHeaders requestHeaders) {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            if (jsonData == null || jsonData.isEmpty()) {
                Result r = new Result("Request body is Empty",  HttpStatus.BAD_REQUEST.value(), "");
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }
            JSONObject jsonPlan = new JSONObject(jsonData);
            String key = objectType + ":" + objectId;

            if (!planService.ifKeyExists(key)) {
                Result r = new Result("Object does not exists",  HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
            }
            if (!jsonValidator.validateJSONSchema(jsonPlan)) {
                Result r = new Result("Payload validation failed", HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }

            String etagFromCache = planService.getETag(key);
            String ifMatchHeader = requestHeaders.getFirst("If-Match");
            String ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
            if(ifMatchHeader.isEmpty()){
                Result r = new Result("Missing Header", HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
            }
            if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                Result r = new Result("Object Modified", HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            }

            if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                Result r = new Result("Object not Modified",  HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            Map<String, Object> existplan = planService.getPlanById(key);
            Map<String, String> actionMap = new HashMap<>();
            actionMap.put("operation", "DELETE");
            actionMap.put("body", new JSONObject(existplan).toString());
            System.out.println("Sending message: " + actionMap);
            template.convertAndSend(RabbitMQConfig.queueName, actionMap);
            planService.delete(key);

            String newEtag = planService.savePlan(jsonPlan, key);

            Map<String, Object> plan = this.planService.getPlanById(key);
            Map<String, String> actionMap1 = new HashMap<>();
            actionMap1.put("operation", "SAVE");
            actionMap1.put("body", new JSONObject(plan).toString());
            template.convertAndSend(RabbitMQConfig.queueName, actionMap1);


            Result r = new Result("Object Updated",  200, plan);
            return ResponseEntity.created(new URI("/" + objectType + "/" + objectId)).eTag(newEtag).body(r);

        } catch (RuntimeException e) {
            Result r = new Result("Error", HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        } catch (URISyntaxException e) {
            Result r = new Result("Invalid request",HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        } catch (FileNotFoundException e) {
            Result r = new Result("File missing for JSON Validation",HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/allPlans")
    public ResponseEntity allPlans(@RequestHeader HttpHeaders requestHeaders) {
        try {
            if(!hasAccess(requestHeaders)){
                Result r = new Result("Unauthorized Access",  HttpStatus.UNAUTHORIZED.value(), new JSONObject().toString());
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }
            List<Plan> plans = planService.getAllPlans();
            if (plans == null || plans.isEmpty()) {
                return new ResponseEntity(new Result("No Plans",  HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>()), HttpStatus.NOT_FOUND);
            }
            Result r = new Result("Success", HttpStatus.OK.value(), plans);
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (Exception e) {
            Result r = new Result("",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
