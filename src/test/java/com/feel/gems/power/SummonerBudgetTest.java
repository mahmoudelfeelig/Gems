package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.summoner.SummonerBudget;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SummonerBudgetTest {
    @Test
    void defaultLoadoutFitsBudget() {
        GemsBalance.Values v = GemsBalance.Values.defaults();
        int cost = SummonerBudget.totalLoadoutCost(v.summoner());
        assertEquals(30, cost);
        assertTrue(cost <= v.summoner().maxPoints());
    }

    @Test
    void missingCostsDoNotContribute() {
        var specs = List.of(new SummonerLoadouts.Entry("minecraft:zombie", 3));
        assertEquals(0, SummonerBudget.slotCost(Map.of(), specs));
        assertEquals(0, SummonerBudget.slotCost(null, specs));
    }
}

