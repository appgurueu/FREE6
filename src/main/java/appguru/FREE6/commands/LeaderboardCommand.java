package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import appguru.FREE6.db.EmbedData;
import appguru.FREE6.db.GuildStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import redis.clients.jedis.Tuple;

import java.util.function.Function;

import static appguru.FREE6.chat.Bot.DISCORD_COLOR;

public class LeaderboardCommand extends Command {

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
        return 1;
    }

    private static void fillLeaderboard(EmbedBuilder eb, Guild g, GuildStorage gs, int lower, int upper, Function<Integer, String> enum_func) {
        int counter = lower;
        for (Tuple t:gs.getLeaderboard(lower, upper)) {
            long id = GuildStorage.byteArrayToLong(t.getBinaryElement());
            double score = t.getScore();
            int level = GuildStorage.getLevel(score);
            double current_score = GuildStorage.LEVELS_INTEGRATED[level];
            score -= current_score;
            int score_to_next = (int)GuildStorage.LEVELS[level];
            eb.appendDescription(enum_func.apply(counter) + " <@" + Long.toUnsignedString(id) + "> • " + "Level " + level + " • " + ((int)score) + "/" + score_to_next + " XP\n");
            counter++;
        }
    }

    public static EmbedBuilder buildLeaderboard(Guild g, GuildStorage gs, long offset, long pages) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("Leaderboard", null, null);
        eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f3c6.png");
        eb.setColor(DISCORD_COLOR);
        if (pages > 0) {
            if (offset == 0) {
                String[] awards = {":first_place_medal:", ":second_place_medal:", ":third_place_medal:"};
                fillLeaderboard(eb, g, gs, 0, 2, (Integer counter) -> awards[counter]);
                fillLeaderboard(eb, g, gs, 3, 10, (Integer counter) -> counter + ". ");
            } else {
                fillLeaderboard(eb, g, gs, (int)offset, (int)offset + 10, (Integer counter) -> counter + ". ");
            }
            eb.setFooter("Page " + (offset / 10 + 1) + "/" + pages, null);
        } else {
            eb.setDescription("Nothing to show.");
        }
        return eb;
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        GuildStorage gs = GuildStorage.get(e.getGuild().getIdLong());
        Long pages = gs.getPages();
        if (pages == null) {
            return;
        }

        long page = 0;
        if (args.length >= 1) {
            try {
                page = Long.parseLong(args[0]);
            } catch (NumberFormatException ex) {
                b.errorReply(e, "No valid integer page number");
                return;
            }
        }

        if (pages != 0) {
            page %= pages;
        } else {
            page = 0;
        }

        final long p = page * 10;
        EmbedBuilder leaderboard = buildLeaderboard(e.getGuild(), gs, p, pages);
        if (leaderboard != null) {
            e.getChannel().sendMessage(leaderboard.build()).queue(m -> {
                m.addReaction("⬅").queue();
                m.addReaction("\uD83D\uDD04").queue();
                m.addReaction("➡").queue();
                gs.setEmbed(m.getChannel().getIdLong(), m.getIdLong(), new EmbedData((byte)0, p).toBytes());
            });
        }
    }
}
