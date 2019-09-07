package appguru.FREE6;

import appguru.FREE6.chat.Bot;
import appguru.FREE6.commands.*;
import appguru.FREE6.db.GuildStorage;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Main {
    public static JDA jda;
    public static Logger LOGGER;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage : <conf> - no arguments given");
            System.exit(1);
        }
        if (args.length > 1) {
            System.out.println("Usage : <conf> - too many arguments given");
            System.exit(1);
        }

        LOGGER = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        LOGGER.setLevel(Level.INFO);

        HashSet<String> required_config_keys = new HashSet();
        required_config_keys.add("discord-token");
        required_config_keys.add("redis-host-ip");
        required_config_keys.add("redis-port");
        required_config_keys.add("redis-enable-ssl");
        required_config_keys.add("redis-password");
        required_config_keys.add("redis-conf");
        HashMap<String, String> config = new HashMap();
        BufferedReader r = new BufferedReader(new FileReader(new File(args[0])));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            String[] conf_and_val = line.split("=", 2);
            String conf = conf_and_val[0].trim();
            String val = conf_and_val[1].trim();
            if (required_config_keys.contains(conf)) {
                if (config.get(conf) != null) {
                    LOGGER.warn("\"" + val + "\" : redefinition, overwriting");
                }
                LOGGER.info("Setting " + conf + " = " + val);
                config.put(conf, val);
            } else {
                LOGGER.warn("\"" + val + "\" : no expected config key");
            }
        }

        String redis_conf = config.get("redis-conf");
        if (redis_conf != null) {
            LOGGER.info("Starting Redis server : redis-server \"" + redis_conf + "\"");
            try {
                Thread t = new Thread(() -> {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("redis-server", redis_conf);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
                    } catch (Exception e) {
                        LOGGER.error("Starting Redis server failed, exiting", e);
                        System.exit(1);
                    }
                });
                t.setName("FREE6.RedisServer");
                t.start();
            } catch (Exception e) {
                LOGGER.error("Starting Redis server failed, exiting", e);
                System.exit(1);
            }
        } else {
            required_config_keys.remove("redis-conf");
        }

        for (String allowed:required_config_keys) {
            if (config.get(allowed) == null) {
                LOGGER.error("Setting \"" + allowed + "\" not given, exiting");
                System.exit(1);
            }
        }
        connecting:
        {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(500);
                    GuildStorage.init(config.get("redis-host-ip"), Integer.parseInt(config.get("redis-port")), Boolean.parseBoolean(config.get("redis-enable-ssl")), config.get("redis-password"));
                    break connecting;
                } catch (Exception e) {
                    LOGGER.error("Connecting to Redis server failed, trying again", e);
                }
            }
            try {
                Thread.sleep(500);
                GuildStorage.init(config.get("redis-host-ip"), Integer.parseInt(config.get("redis-port")), Boolean.parseBoolean(config.get("redis-enable-ssl")), config.get("redis-password"));
            } catch (Exception e) {
                LOGGER.error("Connecting to Redis server failed, exiting", e);
                System.exit(1);
            }
        }
        LOGGER.info("Connected to Redis server");
        JDABuilder builder=new JDABuilder(AccountType.BOT);
        builder.setToken(config.get("discord-token"));
        config = null;
        try {
            jda = builder.build();
            Bot i=new Bot();
            i.registerInfo("about","About","A lightweight level-tracking Discord bot, inspired by [k9](https://github.com/Aurailus/k9) and [MEE6](https://mee6.xyz). Free.").setThumbnail("https://twemoji.maxcdn.com/36x36/1f4c3.png");
            i.registerCommand("help", new HelpCommand());
            RankCommand rank = new RankCommand();
            i.registerCommand("rank", rank);
            i.registerCommand("stats", rank);
            i.registerCommand("status", rank);
            LeaderboardCommand leaderboard = new LeaderboardCommand();
            i.registerCommand("lb", leaderboard);
            i.registerCommand("leaderboard", leaderboard);
            i.registerCommand("import", new ImportCommand());
            i.registerCommand("reset", new ResetCommand());
            RoleCommand role = new RoleCommand();
            i.registerCommand("role", role);
            i.registerCommand("roles", role);
            i.registerCommand("reward", role);
            i.registerCommand("rewards", role);
            i.registerCommand("rolereward", role);
            i.registerCommand("rolerewards", role);
            i.registerCommand("role-reward", role);
            i.registerCommand("role-rewards", role);
            jda.addEventListener(i);
        }
        catch (Exception e) {
            LOGGER.error("Invalid Discord token : Authentication failed !", e);
            System.exit(1);
        }
    }
}
