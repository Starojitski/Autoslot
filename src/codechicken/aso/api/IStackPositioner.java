package codechicken.aso.api;

import codechicken.aso.PositionedStack;

import java.util.ArrayList;

/**
 * For repositioning recipes in overlay renderers.
 * 
 */
public interface IStackPositioner
{
    public ArrayList<PositionedStack> positionStacks(ArrayList<PositionedStack> ai);
}
