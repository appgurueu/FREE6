package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
    public abstract Permission[] getRequiredPermissions();
    public abstract int getMinArgs();
    public abstract int getMaxArgs();
    public abstract void execute(Bot b, MessageReceivedEvent e, String... args);
}
