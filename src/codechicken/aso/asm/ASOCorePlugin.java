package codechicken.aso.asm;

import codechicken.core.launch.CodeChickenCorePlugin;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

import java.io.File;
import java.util.Map;

@TransformerExclusions({"codechicken.aso.asm"})
public class ASOCorePlugin implements IFMLLoadingPlugin, IFMLCallHook
{
    public static File location;

    @Override
    public String[] getASMTransformerClass() {
        CodeChickenCorePlugin.versionCheck(CodeChickenCorePlugin.mcVersion, "AutoSlot");
        return new String[]{"codechicken.aso.asm.ASOTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return "codechicken.aso.ASOModContainer";
    }

    @Override
    public String getSetupClass() {
        return "codechicken.aso.asm.ASOCorePlugin";
    }

    @Override
    public void injectData(Map<String, Object> data) {
        location = (File) data.get("coremodLocation");
        if (location == null)
            location = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public Void call() {
        return null;
    }
}
