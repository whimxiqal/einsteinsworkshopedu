package me.pietelite.einsteinsworkshopedu;

import static me.pietelite.einsteinsworkshopedu.EweduPlugin.ID;
import static me.pietelite.einsteinsworkshopedu.EweduPlugin.VERSION;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.SpongeCommandIssuer;
import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import me.pietelite.einsteinsworkshopedu.features.assignments.Assignment;
import me.pietelite.einsteinsworkshopedu.features.assignments.AssignmentCommand;
import me.pietelite.einsteinsworkshopedu.features.assignments.AssignmentManager;
import me.pietelite.einsteinsworkshopedu.features.boxes.BoxCommand;
import me.pietelite.einsteinsworkshopedu.features.boxes.BoxManager;
import me.pietelite.einsteinsworkshopedu.features.boxes.PlayerLocationManager;
import me.pietelite.einsteinsworkshopedu.features.documentation.DocumentationCommand;
import me.pietelite.einsteinsworkshopedu.features.documentation.DocumentationManager;
import me.pietelite.einsteinsworkshopedu.features.freeze.FreezeCommand;
import me.pietelite.einsteinsworkshopedu.features.freeze.FreezeManager;
import me.pietelite.einsteinsworkshopedu.features.freeze.UnfreezeCommand;
import me.pietelite.einsteinsworkshopedu.features.homes.HomeCommand;
import me.pietelite.einsteinsworkshopedu.features.homes.HomeManager;
import me.pietelite.einsteinsworkshopedu.features.mute.MuteCommand;
import me.pietelite.einsteinsworkshopedu.features.mute.MuteManager;
import me.pietelite.einsteinsworkshopedu.features.mute.UnmuteCommand;
import me.pietelite.einsteinsworkshopedu.features.welcome.WelcomeManager;
import me.pietelite.einsteinsworkshopedu.listeners.ChangeBlockListener;
import me.pietelite.einsteinsworkshopedu.listeners.ChatListener;
import me.pietelite.einsteinsworkshopedu.listeners.InteractEventListener;
import me.pietelite.einsteinsworkshopedu.listeners.LoginEventListener;
import me.pietelite.einsteinsworkshopedu.listeners.TargetEntityEventListener;
import me.pietelite.einsteinsworkshopedu.listeners.TargetInventoryEventListener;
import me.pietelite.einsteinsworkshopedu.tools.Feature;

import me.pietelite.einsteinsworkshopedu.tools.chat.Menu;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.TargetInventoryEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

/**
 * The main class for the EDU plugin for Einstein's Workshop.
 * This plugin is made exclusively for Einstein's Workshop
 * Do not use this plugin without explicit approval from a member of staff from
 * Einstein's Workshop.
 *
 * <p><b><u>Permissions</u></b>
 *
 * <li><i>einsteinsworkshop.student</i>: Allows the use of the student-level abilities</li>
 * <li><i>einsteinsworkshop.instructor</i>: Allows the use of all instructor-level abilities</li>
 * <li><i>einsteinsworkshop.immunity</i>: Makes the player immune to student-altering effects</li>
 */
@Plugin(id = ID,
    name = "EinsteinsWorkshopEDU",
    version = VERSION,
    description = "Education Administrative Tool")
public class EweduPlugin implements PluginContainer {

  static final String VERSION = "1.0";
  static final String ID = "einsteinsworkshopedu";

  public static final String DOCUMENTATION_LINK = "https://github.com/pietelite/einsteinsworkshopedu";

  /**
   * Location of the default configuration file for this plugin. From Sponge API.
   */
  @Inject
  @DefaultConfig(sharedRoot = false)
  private Path defaultConfig;

  /**
   * Configuration manager of the configuration file. From Sponge API.
   */
  @Inject
  @DefaultConfig(sharedRoot = false)
  private ConfigurationLoader<CommentedConfigurationNode> configManager;
  /**
   * The root node of the configuration file, using the configuration manager.
   */
  private ConfigurationNode rootNode;

  @Inject
  @ConfigDir(sharedRoot = false)
  private File configDirectory;

  @Inject
  private PluginContainer container;

  private SpongeCommandManager commandManager;

  private HashMap<FeatureTitle, Feature> features = new HashMap<>();

  private PlayerLocationManager playerLocationManager;

  private static List<String> assignmentTypes;

  /**
   * Run initialization sequence before the game starts.
   * All classes that other classes depend on must be initialized here.
   *
   * @param event the event run before the game starts
   */
  @Listener
  public void onInitialize(GameInitializationEvent event) {
    getLogger().info("Initializing EinsteinsWorkshopEdu...");


    playerLocationManager = new PlayerLocationManager();

    // Init the config from the Sponge API and set the specific node values.
    initializeConfig();
    if (!this.getDataDirectory().mkdir()) {
      getLogger().info("The data directory could not be created. Is it already there?");
    }

    // Classes which other classes depend on must be initialized here.

    // Register all the listeners with Sponge
    registerListeners();

  }

  /**
   * Run game starting sequence before the game starts.
   *
   * @param event the event run before the game starts
   */
  @Listener
  public void onStarted(GameStartedServerEvent event) {
    // Register all the features with Sponge (including commands)
    // Do this here instead of onInitialize because some features have
    // to know what the worlds are, which is known to the plugin after
    // initialization
    initializeFeatures();
    loadConfig();
  }

  private void initializeFeatures() {
    initCommands();
    features.put(
        FeatureTitle.FREEZE,
        new Feature(this,
            new FreezeManager(this),
            new EinsteinsWorkshopCommand[]{
                new FreezeCommand(this),
                new UnfreezeCommand(this)
            }));
    features.put(
        FeatureTitle.ASSIGNMENTS,
        new Feature(this,
            new AssignmentManager(this),
            new EinsteinsWorkshopCommand[]{
                new AssignmentCommand(this)
            }));
    features.put(
        FeatureTitle.BOXES,
        new Feature(this,
            new BoxManager(this),
            new EinsteinsWorkshopCommand[]{
                new BoxCommand(this)
            }));
    features.put(
        FeatureTitle.HOMES,
        new Feature(this,
            new HomeManager(this),
            new EinsteinsWorkshopCommand[]{
                new HomeCommand(this)
            }));
    features.put(
        FeatureTitle.MUTE,
        new Feature(this,
            new MuteManager(this),
            new EinsteinsWorkshopCommand[]{
                new MuteCommand(this),
                new UnmuteCommand(this)
            }));
    features.put(
        FeatureTitle.DOCUMENTATION,
        new Feature(this,
            new DocumentationManager(this),
            new EinsteinsWorkshopCommand[]{
                new DocumentationCommand(this)
            }));
    features.put(
        FeatureTitle.WELCOME,
        new Feature(this,
            new WelcomeManager(this)
        )
    );
  }

  @SuppressWarnings("deprecation")
  private void initCommands() {
    commandManager = new SpongeCommandManager(this.container);
    commandManager.enableUnstableAPI("help");
    commandManager.createRootCommand("einsteinsworkshop");
    commandManager.registerCommand(new EinsteinsWorkshopCommand(this, Menu.Section.NONE));
    registerConditions();
    registerCompletions();
  }

  private void initializeConfig() {
    if (!defaultConfig.toFile().exists()) {
      getLogger().info("Generating New Configuration File...");
      try {
        rootNode = configManager.load();
        ConfigurationNode featureNode = rootNode.getNode("features");
        for (FeatureTitle feature : FeatureTitle.values()) {
          featureNode.getNode(feature.name()).getNode("enabled").setValue(true);
        }
        featureNode.getNode(FeatureTitle.ASSIGNMENTS.name()).getNode("types")
            .setValue(Assignment.DEFAULT_ASSIGNMENT_TYPES);
        configManager.save(rootNode);
        getLogger().info("New Configuration File created successfully!");
      } catch (IOException e) {
        getLogger().warn("Exception while reading configuration", e);
      }
    }
  }

  private void loadConfig() {
    try {
      rootNode = configManager.load();
      ConfigurationNode featureNode = rootNode.getNode("features");
      for (FeatureTitle feature : getFeatures().keySet()) {
        getFeatures().get(feature).isEnabled = featureNode.getNode(feature.name())
            .getNode("enabled").getBoolean();
      }
      assignmentTypes = featureNode.getNode(FeatureTitle.ASSIGNMENTS.name())
          .getNode("types").getList((object) -> {
            if (object == null) {
              return null;
            } else {
              return object.toString();
            }
          });
      getLogger().info("List of Valid Assignment Types: " + assignmentTypes.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void registerConditions() {
    // @Condition("player)
    commandManager.getCommandConditions().addCondition("player", (context) -> {
      SpongeCommandIssuer issuer = context.getIssuer();
      if (!(issuer.isPlayer())) {
        throw new ConditionFailedException("You must be a player to execute this command.");
      }
    });
  }

  private void registerCompletions() {
    commandManager.getCommandCompletions().registerCompletion("players", c -> {
      List<String> onlinePlayerNames = new ArrayList<>();
      for (Player player : Sponge.getServer().getOnlinePlayers()) {
        onlinePlayerNames.add(player.getName());
      }
      return onlinePlayerNames;
    });
    commandManager.getCommandCompletions().registerCompletion("assignment_types", c -> {
      try {
        return EweduPlugin.getAssignmentTypes();
      } catch (Exception e) {
        e.printStackTrace();
        return new LinkedList<>();
      }
    });
  }


  private void registerListeners() {
    Sponge.getEventManager().registerListener(this, TargetEntityEvent.class, Order.LAST,
        new TargetEntityEventListener(this));
    Sponge.getEventManager().registerListener(this, InteractEvent.class, Order.LAST,
        new InteractEventListener(this));
    Sponge.getEventManager().registerListener(this, TargetInventoryEvent.class, Order.LAST,
        new TargetInventoryEventListener(this));
    Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Join.class, Order.LAST,
        new LoginEventListener(this));
    Sponge.getEventManager().registerListener(this, MessageChannelEvent.Chat.class, Order.LAST,
        new ChatListener(this));
    Sponge.getEventManager().registerListener(this, ChangeBlockEvent.class, Order.FIRST,
        new ChangeBlockListener(this));
  }

  /**
   * To be run when the plugin reloads.
   *
   * @param event The GameReloadEvent
   */
  @Listener
  public void onReload(GameReloadEvent event) {
    getLogger().info("Reloading EinsteinsWorkshopEDU config data");
    try {
      rootNode = configManager.load();
    } catch (IOException e) {
      getLogger().warn("Exception while reading configuration", e);
    }
    getLogger().info("EinsteinsWorkshopEDU config data reloaded!");
  }

  @Listener
  public void onGameLoadCompleteEvent(GameLoadCompleteEvent event) {
    getLogger().info("Find documentation here: ");
    getLogger().info(DOCUMENTATION_LINK);
  }

  public HashMap<FeatureTitle, Feature> getFeatures() {
    return features;
  }

  public PlayerLocationManager getPlayerLocationManager() {
    return playerLocationManager;
  }

  public File getDataDirectory() {
    return new File(configDirectory.getParentFile().getParentFile().getPath() + "/" + ID);
  }

  public SpongeCommandManager getCommandManager() {
    return commandManager;
  }

  public static List<String> getAssignmentTypes() {
    return assignmentTypes;
  }

  @Override
  @SuppressWarnings("all")
  public String getId() {
    return ID;
  }

  public File getConfigDirectory() {
    return configDirectory;
  }

  public enum FeatureTitle {
    BOXES,
    ASSIGNMENTS,
    FREEZE,
    HOMES,
    MUTE,
    DOCUMENTATION,
    WELCOME
  }

  public enum Permissions {
    INSTRUCTOR {
      public String toString() {
        return "einsteinsworkshop.instructor";
      }
    },
    STUDENT {
      public String toString() {
        return "einteinsworkshop.student";
      }
    },
    IMMUNITY {
      public String toString() {
        return "einsteinsworkshop.immunity";
      }
    }
  }

}
