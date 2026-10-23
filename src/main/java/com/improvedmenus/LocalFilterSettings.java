package com.improvedmenus;

import com.improvedmenus.rs2f.DisplayConfig;
import com.improvedmenus.rs2f.FilterRule;
import com.improvedmenus.rs2f.LootFilter;
import com.improvedmenus.rs2f.ast.AndCondition;
import com.improvedmenus.rs2f.ast.Condition;
import com.improvedmenus.rs2f.ast.OrCondition;
import com.improvedmenus.rs2f.ast.leaf.ItemNameCondition;
import com.improvedmenus.rs2f.ast.leaf.ItemQuantityCondition;
import com.improvedmenus.rs2f.model.Comparator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;
import net.runelite.client.config.ConfigManager;

/** Read-only snapshot of settings that affect icons and menu display in released Loot Filters. */
@Value
final class LocalFilterSettings
{
    static final String GROUP = "loot-filters";
    String selected;
    boolean compact;
    String highlighted, hidden;
    Color highlightColor, highlightMenuColor;
    int highlightSort;

    static LocalFilterSettings read(ConfigManager configs)
    {
        return new LocalFilterSettings(configs.getConfiguration(GROUP, "selected-filter"),
            Boolean.parseBoolean(configs.getConfiguration(GROUP, "compactMode")),
            text(configs, "highlightedItems"), text(configs, "hiddenItems"),
            color(configs, "highlightColor", new Color(0xaa00ff)), color(configs, "hdMenuTextColor", null),
            number(configs.getConfiguration(GROUP, "hdMenuSort")));
    }

    private static String text(ConfigManager configs, String key)
    {
        String value = configs.getConfiguration(GROUP, key);
        return value == null ? "" : value;
    }

    private static Color color(ConfigManager configs, String key, Color fallback)
    {
        try
        {
            Color value = configs.getConfiguration(GROUP, key, Color.class);
            return value == null ? fallback : value;
        }
        catch (RuntimeException ex) { return fallback; }
    }

    private static int number(String value)
    {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { return 0; }
    }

    LootFilter decorate(LootFilter script)
    {
        // Same precedence as FilterUtil.withConfigRules: highlight, hide, then script.
        LootFilter.Builder builder = LootFilter.builder();
        builder.addRule(new FilterRule(names(highlighted), DisplayConfig.builder()
            .textColor(highlightColor).menuTextColor(highlightMenuColor).menuSort(highlightSort).build(), true, -3));
        builder.addRule(new FilterRule(names(hidden), DisplayConfig.builder().hidden(true).build(), true, -4));
        for (FilterRule rule : script.rules) builder.addRule(rule);
        return builder.build();
    }

    /** Adapted from Loot Filters' NamedQuantity / FilterRule (bundled BSD-2-Clause license). */
    private static Condition names(String raw)
    {
        List<Condition> names = new ArrayList<>();
        if (raw.length() > 200000) throw new IllegalArgumentException("Item list is too large");
        for (String part : raw.split(","))
        {
            String name = part;
            Comparator comparator = Comparator.GT;
            int quantity = 0;
            String op = part.contains(">") ? ">" : part.contains("<") ? "<" : null;
            if (op != null)
            {
                String[] pieces = part.split(op);
                if (pieces.length == 2)
                {
                    try
                    {
                        quantity = Integer.parseInt(pieces[1].trim());
                        name = pieces[0].trim();
                        comparator = Comparator.fromString(op);
                    }
                    catch (NumberFormatException ignored) { }
                }
            }
            names.add(new AndCondition(new ItemNameCondition(name), new ItemQuantityCondition(quantity, comparator)));
        }
        return new OrCondition(names);
    }
}
