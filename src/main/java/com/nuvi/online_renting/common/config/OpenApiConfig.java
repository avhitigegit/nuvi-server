package com.nuvi.online_renting.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NUVI Online Renting API")
                        .version("1.0.0")
                        .description("""
                                REST API for the **NUVI Online Renting Platform**.

                                This platform allows users to browse and rent items listed by sellers.
                                Admins manage the full platform including users, bookings, and seller approvals.

                                **How to authenticate:**
                                1. Call `POST /api/auth/login` with your email and password.
                                2. Copy the `token` from the response.
                                3. Click the **Authorize** button above, enter: `Bearer <your_token>`, and click Authorize.
                                4. All secured endpoints will now include your token automatically.

                                **Roles:**
                                - `USER` — Can browse items, create bookings, and manage their own profile.
                                - `SELLER` — Everything a USER can do, plus create and manage their own item listings.
                                - `ADMIN` — Full access to all resources including user and seller management.
                                """)
                        .contact(new Contact()
                                .name("NUVI Development Team")
                                .email("amrithagz123@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter the JWT token obtained from /api/auth/login. Format: Bearer <token>")));
    }
}
