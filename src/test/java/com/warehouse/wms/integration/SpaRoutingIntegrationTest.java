package com.warehouse.wms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void authenticatedUserCanAccessSpaRoutes() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/locations")).andExpect(status().isOk());
        mockMvc.perform(get("/map")).andExpect(status().isOk());
        mockMvc.perform(get("/picking")).andExpect(status().isOk());
        mockMvc.perform(get("/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(get("/profile")).andExpect(status().isOk());
    }

    @Test
    void publicStaticResourcesAndDocsAreAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
