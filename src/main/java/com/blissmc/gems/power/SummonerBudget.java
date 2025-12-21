package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;

import java.util.List;
import java.util.Map;

public final class SummonerBudget {
    private SummonerBudget() {
    }

    public static int totalLoadoutCost(GemsBalance.Summoner cfg) {
        return slotCost(cfg.costs(), cfg.slot1())
                + slotCost(cfg.costs(), cfg.slot2())
                + slotCost(cfg.costs(), cfg.slot3())
                + slotCost(cfg.costs(), cfg.slot4())
                + slotCost(cfg.costs(), cfg.slot5());
    }

    public static int slotCost(Map<String, Integer> costs, List<com.feel.gems.config.GemsBalanceConfig.Summoner.SummonSpec> specs) {
        if (costs == null || costs.isEmpty() || specs == null || specs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (var spec : specs) {
            if (spec == null || spec.entityId == null) {
                continue;
            }
            Integer cost = costs.get(spec.entityId);
            if (cost == null || cost <= 0) {
                continue;
            }
            total += cost * Math.max(0, spec.count);
        }
        return total;
    }
}

