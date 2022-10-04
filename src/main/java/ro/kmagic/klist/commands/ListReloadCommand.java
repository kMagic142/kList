package ro.kmagic.klist.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import ro.kmagic.klist.kList;

public class ListReloadCommand extends Command {

    public ListReloadCommand() {
        super("glist-reload", "bungeecord.command.reload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        kList.reloadConfig();
        sender.sendMessage("Configuratia a fost reincarcata cu succes. (Module by kMagic)");

    }

}
