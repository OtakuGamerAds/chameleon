package mchorse.chameleon.commands;

import mchorse.chameleon.Chameleon;
import mchorse.chameleon.ClientProxy;
import mchorse.chameleon.lib.ChameleonModel;
import mchorse.chameleon.metamorph.ChameleonSection;
import mchorse.mclib.utils.files.entries.FolderEntry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;
import java.util.concurrent.*;

/* CLASS COPIED FROM BB */
/* HEAVILY MODIFIED */
public class SubCommandChameleonModelReload extends ChameleonCommandBase{
    private final ConcurrentHashMap<String, FolderEntry> cache = new ConcurrentHashMap<>();

    @Override
    public String getName()
    {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "chameleon.commands.model.reload";
    }

    @Override
    public String getSyntax()
    {
        return "{l}{6}/{r}model {8}reload{r} {7}[force]{r}";
    }

    @Override
    public void executeCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        boolean force = args.length >= 1 && CommandBase.parseBoolean(args[0]);

        /* Clear models */
        Chameleon.proxy.clearModels();

        /* Reload models */
        Chameleon.proxy.reloadModels();

        /* Reload skins */
        long startTime = System.nanoTime();
        ///////////////
        HashMap<String, FolderEntry> skinsMap = new HashMap<>();

        for (String key : Chameleon.proxy.getModelKeys()){
            try {
                skinsMap.putIfAbsent(key, getFolderEntryFromCache(key));
            } catch (Exception e) {
                // Log the error and continue with the next iteration
                System.err.println("Error fetching folder entry for key: " + key);
                e.printStackTrace();
            }
        }
        ChameleonSection.skinsMap = skinsMap;
        //////////////
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0; // divide by 1 billion to get seconds
        broadcastMessage("Models were reloaded in: " + durationInSeconds + " seconds");
    }

    private FolderEntry getFolderEntryFromCache(String key) {
        if (!cache.containsKey(key)) {
            ChameleonModel model = ClientProxy.chameleonModels.get(key);
            if (model != null && model.thumbnailFullPath == null) {
                FolderEntry folderEntry = ClientProxy.tree.getByPath(key + "/skins/", null);
                cache.put(key, folderEntry);
            }
        }
        return cache.get(key);
    }


    private void broadcastMessage(String message) {
        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();

        for (EntityPlayerMP player : players) {
            player.sendMessage(new TextComponentString(message));
        }
    }
}
