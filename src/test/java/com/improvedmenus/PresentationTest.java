package com.improvedmenus;

import net.runelite.api.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresentationTest
{
    @Test public void additionalOptionsExcludeWalkCancelAndLocalControls()
    {
        assertEquals(1,MenuPresentation.additionalCount(new MenuEntry[]{
            TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0),TestMenus.entry("Walk here","",MenuAction.WALK,-1,0),
            TestMenus.entry("Colors","",MenuAction.RUNELITE,-1,0),TestMenus.entry("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,-1,0),
            TestMenus.entry("Examine","Coins",MenuAction.EXAMINE_ITEM_GROUND,-1,0)}));
    }
    @Test public void countNeverNegative() { assertEquals(0,MenuPresentation.additionalCount(new MenuEntry[0])); }
}
