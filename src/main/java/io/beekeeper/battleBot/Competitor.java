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

    private final String Description;

    private final String winRate;
}
