// Adapted from riktenx/loot-filters (BSD-2-Clause); see loot-filters-LICENSE.txt.
package com.improvedmenus.rs2f.ast;

import com.improvedmenus.rs2f.FilterContext;
import com.improvedmenus.rs2f.model.ItemFacts;

public abstract class Condition {
    public abstract boolean test(FilterContext plugin, ItemFacts item);
}
