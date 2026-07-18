package com.commercehub.notification.controller;

import com.commercehub.notification.dto.SendNotificationRequest;
import com.commercehub.notification.dto.SendNotificationResponse;
import com.commercehub.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Nested
    class Send {

        @Test
        void send_delegatesToServiceAndReturnsOk() throws Exception {
            when(notificationService.send(any(SendNotificationRequest.class)))
                    .thenReturn(new SendNotificationResponse(true));

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SendNotificationRequest(
                                    "user@example.com",
                                    "Order Created",
                                    "Your order has been created."
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).send(any(SendNotificationRequest.class));
        }

        @Test
        void send_passesRequestFieldsToService() throws Exception {
            SendNotificationRequest request = new SendNotificationRequest(
                    "buyer@example.com",
                    "Order Shipped",
                    "Your package is on the way."
            );
            when(notificationService.send(request)).thenReturn(new SendNotificationResponse(true));

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(notificationService).send(request);
        }

        @Test
        void send_blankEmail_returnsBadRequestWithoutCallingService() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "",
                                      "subject": "Order Created",
                                      "message": "Your order has been created."
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).send(any());
        }

        @Test
        void send_missingSubject_returnsBadRequestWithoutCallingService() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "user@example.com",
                                      "message": "Your order has been created."
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).send(any());
        }

        @Test
        void send_missingMessage_returnsBadRequestWithoutCallingService() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "user@example.com",
                                      "subject": "Order Created"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).send(any());
        }
    }
}
