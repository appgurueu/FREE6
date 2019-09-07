package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static appguru.FREE6.chat.Bot.DISCORD_COLOR;

public class HelpCommand extends Command {
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

    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        MessageChannel c=e.getChannel();
        EmbedBuilder helpEmbed=new EmbedBuilder();
        helpEmbed.setTitle("Help",b.README_URL +"#help");
        helpEmbed.setThumbnail("https://twemoji.maxcdn.com/36x36/2139.png");
        helpEmbed.setDescription("**Entering commands :**\n" +
                "Enter one of the commands below, prefixed with `"+b.prefix+"` and if required, followed by a list of arguments, separated with whitespaces.\n" +
                "**Informational commands :** \n"+
                "• `about` : Shows general info about the bot\n"+
                "• `help` : Shows this help dialog\n"+
                "**Ranking commands :** \n"+
                "• `rank [mention/id]` : Shows somebody's rank\n"+
                "• `leaderboard/lb [page]` : Shows an interactive leaderboard\n"+
                "**Administrative commands :** \n" +
                "• `import [url/id]` : Import XP from a MEE6 leaderboard\n" +
                "• `reset` : Reset levels, needs to be called a 2nd time to confirm\n" +
                "• `role [level] [mention/id/name]` : Get/set a role reward");
        helpEmbed.setColor(DISCORD_COLOR);
        c.sendMessage(helpEmbed.build()).queue();
    }
}
