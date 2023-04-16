package info.neu.infoapp.service;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface JsonValidatorService {
    public boolean validateJSONSchema(JSONObject data) throws FileNotFoundException;
}
