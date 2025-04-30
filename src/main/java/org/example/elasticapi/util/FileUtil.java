package org.example.elasticapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

public class FileUtil {
    public Map<String, Object> getFileContent(String fileName) {
        String str;
        StringBuilder stringBuilder = new StringBuilder(4000);
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader((new ClassPathResource(fileName)).getInputStream())
        )) {
            while ((str = bufferedReader.readLine()) != null) {
                stringBuilder.append(str);
            }
            return new ObjectMapper().readValue(stringBuilder.toString(), Map.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}