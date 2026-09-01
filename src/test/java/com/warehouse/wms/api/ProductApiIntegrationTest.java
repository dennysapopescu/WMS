package com.warehouse.wms.api;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired LocationRepository locationRepository;
    @Autowired ProductRepository productRepository;
    private Long locationId;

    @BeforeEach void setUp() {
        productRepository.deleteAll(); locationRepository.deleteAll();
        Location location = new Location(); location.setCode("A-01"); location.setDescription("Aisle A"); location.setMaxCapacity(20); location.setCurrentOccupancy(0);
        locationId = locationRepository.save(location).getId();
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void createsProductAndExposesDtoWithoutJpaGraph() throws Exception {
        String payload = "{\"sku\":\"SKU-100\",\"name\":\"Scanner\",\"quantity\":4,\"price\":50.0,\"locationId\":" + locationId + "}";
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/products/")))
                .andExpect(jsonPath("$.sku").value("SKU-100")).andExpect(jsonPath("$.locationCode").value("A-01"));
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].quantity").value(4));
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void rejectsInvalidPayloadWithStructuredValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"\",\"quantity\":-1,\"price\":-1,\"locationId\":null}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.sku").exists());
    }

    @Test void exposesOpenApiSpecificationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("WMS REST API"));
    }
}
