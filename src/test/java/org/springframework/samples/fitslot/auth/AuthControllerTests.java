package org.springframework.samples.fitslot.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatesSeededReceptionist() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "receptionist1",
                      "password": "Receptionist123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("receptionist1"))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem("RECEPTIONIST")));
    }
}
