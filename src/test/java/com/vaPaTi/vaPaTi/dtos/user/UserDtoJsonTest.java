package com.vaPaTi.vaPaTi.dtos.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDTO JSON Tests")
class UserDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("null socialLinks and tags: the keys are omitted")
    void serialize_WithNullLists_ShouldOmitKeys() {
        JsonNode json = objectMapper.valueToTree(UserDTO.builder().id(1L).build());

        assertThat(json.has("socialLinks")).isFalse();
        assertThat(json.has("tags")).isFalse();
    }

    @Test
    @DisplayName("empty socialLinks and tags: the keys are present")
    void serialize_WithEmptyLists_ShouldIncludeKeys() {
        JsonNode json = objectMapper.valueToTree(UserDTO.builder().id(1L).socialLinks(List.of()).tags(List.of()).build());

        assertThat(json.has("socialLinks")).isTrue();
        assertThat(json.get("socialLinks").isArray()).isTrue();
        assertThat(json.has("tags")).isTrue();
        assertThat(json.get("tags")).isEmpty();
    }
}
