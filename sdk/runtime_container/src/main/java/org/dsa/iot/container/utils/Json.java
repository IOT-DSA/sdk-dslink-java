package org.dsa.iot.container.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Samuel Grenier
 */
public class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
