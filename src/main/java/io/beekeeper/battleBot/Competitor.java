package io.beekeeper.battleBot;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Data
public class Competitor {
    private final String name;

    private final List<String> commands;

    private final String description;

    private final String winRate;

    private final List<String> urls;
}
