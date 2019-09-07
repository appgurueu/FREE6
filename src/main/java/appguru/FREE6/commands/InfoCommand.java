package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class InfoCommand extends Command {
    public EmbedBuilder embed;
    public InfoCommand(EmbedBuilder eb) {
        embed = eb;
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[0];
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        e.getChannel().sendMessage(embed.build()).queue();
    }
}
