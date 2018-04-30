package codechicken.aso.api;

import codechicken.aso.recipe.IRecipeHandler;
import net.minecraft.client.gui.inventory.GuiContainer;

public interface IOverlayHandler
{
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift);
}
