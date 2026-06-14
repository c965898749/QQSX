package com.sy.model.game;

import lombok.Data;

import java.util.Map;

@Data
public class Battle {
    private Integer isWin;
    private String id;
    private String chapter;
    private Map json;
}
