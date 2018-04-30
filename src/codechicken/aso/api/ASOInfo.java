package codechicken.aso.api;

import codechicken.aso.ASOClientConfig;
import codechicken.aso.config.OptionCycled;
import net.minecraft.world.World;

import java.util.LinkedList;

public class ASOInfo
{
    public static final LinkedList<IASOModeHandler> modeHandlers = new LinkedList<IASOModeHandler>();

    public static void load(World world) {
        OptionCycled modeOption = (OptionCycled) ASOClientConfig.getOptionList().getOption("inventory.cheatmode");
        modeOption.parent.synthesizeEnvironment(false);
        if(!modeOption.optionValid(modeOption.value())) {
            modeOption.copyGlobals();
            modeOption.cycle();
        }
    }

    public static boolean isValidMode(int mode) {
        for(IASOModeHandler handler : modeHandlers)
            if(!handler.isModeValid(mode))
                return false;
        
        return true;
    }
}
