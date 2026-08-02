package shit.zen.command.impl;

import java.io.IOException;
import java.util.List;
import shit.zen.ZenClient;
import shit.zen.command.Command;
import shit.zen.manager.ConfigManager;
import shit.zen.utils.misc.ChatUtil;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", new String[]{"cfg"});
    }

    @Override
    public void onCommand(String[] args) {
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();

        if (args.length == 0) {
            printUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                configManager.loadAll();
                ChatUtil.print("Config reloaded!");
                break;

            case "folder":
                try {
                    Runtime.getRuntime().exec("explorer " + ConfigManager.CONFIG_DIR.getAbsolutePath());
                } catch (IOException ignored) {
                }
                break;

            case "list":
                List<String> profiles = configManager.listProfiles();
                String active = configManager.getActiveProfile();
                if (profiles.isEmpty()) {
                    ChatUtil.print("No profiles found. Current config is default.");
                } else {
                    ChatUtil.print("Profiles:");
                    for (String name : profiles) {
                        boolean isActive = name.equals(active);
                        ChatUtil.print("  " + (isActive ? "[*] " : "[ ] ") + name);
                    }
                }
                break;

            case "save":
                if (args.length < 2) {
                    ChatUtil.print("Usage: config save <name>");
                    return;
                }
                configManager.saveProfile(args[1]);
                ChatUtil.print("Saved profile: " + args[1]);
                break;

            case "load":
                if (args.length < 2) {
                    ChatUtil.print("Usage: config load <name>");
                    return;
                }
                configManager.loadProfile(args[1]);
                ChatUtil.print("Loaded profile: " + args[1]);
                break;

            case "delete":
                if (args.length < 2) {
                    ChatUtil.print("Usage: config delete <name>");
                    return;
                }
                configManager.deleteProfile(args[1]);
                ChatUtil.print("Deleted profile: " + args[1]);
                break;

            default:
                printUsage();
                break;
        }
    }

    private void printUsage() {
        ChatUtil.print("Usage:");
        ChatUtil.print("  config reload");
        ChatUtil.print("  config folder");
        ChatUtil.print("  config list");
        ChatUtil.print("  config save <name>");
        ChatUtil.print("  config load <name>");
        ChatUtil.print("  config delete <name>");
    }

    @Override
    public String[] onTab(String[] args) {
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();

        if (args.length == 0 || args.length == 1) {
            return new String[]{"reload", "folder", "list", "save", "load", "delete"};
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("load") || sub.equals("delete")) {
                List<String> profiles = configManager.listProfiles();
                return profiles.toArray(new String[0]);
            }
        }

        return new String[0];
    }
}
