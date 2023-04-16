package info.neu.infoapp.controller;

import info.neu.infoapp.model.Result;
import info.neu.infoapp.service.AuthorizationServiceImpl;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;


@RestController
@RequiredArgsConstructor
public class AuthorizationController {
    private final AuthorizationServiceImpl authorizationService;

    @RequestMapping(method = RequestMethod.GET, value = "/token")
    ResponseEntity getToken() {
        String token;
        try {
            token = authorizationService.generateToken();
            JSONObject obj = new JSONObject();
            obj.put("token", token);
            Result r = new Result("Token successfully created!!",  HttpStatus.CREATED.value(), obj);
            return new ResponseEntity<>(r, HttpStatus.CREATED);
        } catch (Exception e) {
            Result r = new Result("Bad Request",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(method = RequestMethod.POST, value = "/validate")
    ResponseEntity validateToken(@RequestHeader(value = "Authorization") String token) {

        try {
            String isToken = authorizationService.authorize(token);
            Result r;
            if (isToken.equals("VALID TOKEN")) {
                r = new Result("Token verified!!", 200, true);
            }
            else if (isToken.equals("TOKEN EXPIRED")) {
                r = new Result("Token Expired!!", 401, false);
                return new ResponseEntity<>(r, HttpStatus.UNAUTHORIZED);
            }else {
                r = new Result(isToken,  200, false);
            }
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (Exception e) {
            Result r = new Result("Bad Request", 404, new ArrayList<>());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
