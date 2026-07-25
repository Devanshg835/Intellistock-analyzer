package com.intellistock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellistock.dto.AnalyzeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class StockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAnalyzeEndpoint_ValidSymbol_ReturnsReport() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSymbol("INFY");

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("INFY")))
                .andExpect(jsonPath("$.companyName", notNullValue()))
                .andExpect(jsonPath("$.overallScore", notNullValue()))
                .andExpect(jsonPath("$.recommendation", notNullValue()));
    }

    @Test
    void testAnalyzeEndpoint_EmptySymbol_ReturnsBadRequest() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSymbol(""); // empty symbol triggers @Valid fail

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHistoryEndpoint_ValidSymbol_ReturnsHistoryArray() throws Exception {
        mockMvc.perform(get("/api/stocks/AAPL/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.history", hasSize(31)));
    }

    @Test
    void testWatchlistFlow_GetAddRemove_Succeeds() throws Exception {
        // 1. Verify GET watchlist starts empty or succeeds
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk());

        // 2. Add TSLA to watchlist
        String addPayload = "{\"symbol\":\"TSLA\"}";
        mockMvc.perform(post("/api/watchlist/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(addPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")));

        // 3. Verify watched check is true
        mockMvc.perform(get("/api/watchlist/check/TSLA"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 4. Remove TSLA from watchlist
        mockMvc.perform(delete("/api/watchlist/remove/TSLA"))
                .andExpect(status().isOk());

        // 5. Verify watched check is false
        mockMvc.perform(get("/api/watchlist/check/TSLA"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
