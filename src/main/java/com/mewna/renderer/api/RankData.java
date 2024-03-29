package com.mewna.renderer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 4/10/19.
 */
@Getter
@Setter
@Accessors(fluent = true)
@NoArgsConstructor
public class RankData {
    @JsonProperty
    private String id;
    @JsonProperty
    private String background;
    @JsonProperty
    private String avatarUrl;
    @JsonProperty
    private String username;
    @JsonProperty
    private long exp;
    @JsonProperty
    private long rank;
}
