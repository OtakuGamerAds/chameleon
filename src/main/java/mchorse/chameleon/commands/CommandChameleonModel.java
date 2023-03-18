package mchorse.chameleon.commands;

import mchorse.chameleon.Chameleon;
import mchorse.mclib.commands.SubCommandBase;
import mchorse.mclib.commands.utils.L10n;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/* CLASS COPIED FROM BB */
/**
 * Command /model
 *
 * Another client-side command which is responsible for
 */
public class CommandChameleonModel extends SubCommandBase{
    /**
     * Model command's constructor
     *
     * This method is responsible for attaching sub commands for this model
     */
    public CommandChameleonModel()
    {
        this.add(new SubCommandChameleonModelReload());
    }

    @Override
    public L10n getL10n()
    {
        return Chameleon.l10n;
    }

    @Override
    public String getName()
    {
        return "model";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "chameleon.commands.model.help";
    }

    @Override
    public String getSyntax()
    {
        return "";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }
}
