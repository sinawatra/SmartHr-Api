/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 *
 * @author sinawatrarith
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Smart HR API",
        version = "1.0",
        description = "API documentation for Smart HR system"
    )
)
public class OpenApiConfig {

}
