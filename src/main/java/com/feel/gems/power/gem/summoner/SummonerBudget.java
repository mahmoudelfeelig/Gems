package com.feel.gems.power.gem.summoner;

import com.feel.gems.config.GemsBalance;
import java.util.List;
import java.util.Map;


public final class SummonerBudget {
    private SummonerBudget() {
    }

    public static int totalLoadoutCost(GemsBalance.Summoner cfg) {
        return totalLoadoutCost(cfg.costs(), SummonerLoadouts.fromConfig(cfg));
    }

    public static int totalLoadoutCost(Map<String, Integer> costs, SummonerLoadouts.Loadout loadout) {
        return slotCost(costs, loadout.slot1())
                + slotCost(costs, loadout.slot2())
                + slotCost(costs, loadout.slot3())
                + slotCost(costs, loadout.slot4())
                + slotCost(costs, loadout.slot5());
    }

    public static int slotCost(Map<String, Integer> costs, List<SummonerLoadouts.Entry> specs) {
        if (costs == null || costs.isEmpty() || specs == null || specs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (SummonerLoadouts.Entry spec : specs) {
            if (spec == null || spec.entityId() == null) {
                continue;
            }
            Integer cost = costs.get(spec.entityId());
            if (cost == null || cost <= 0) {
                continue;
            }
            total += cost * Math.max(0, spec.count());
        }
        return total;
    }
}

