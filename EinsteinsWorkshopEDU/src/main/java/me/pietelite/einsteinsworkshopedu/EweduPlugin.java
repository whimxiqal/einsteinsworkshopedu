package me.pietelite.einsteinsworkshopedu;
import static me.pietelite.einsteinsworkshopedu.EweduPlugin.VERSION;
import static me.pietelite.einsteinsworkshopedu.EweduPlugin.ID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import me.pietelite.einsteinsworkshopedu.features.FeatureManager;
import me.pietelite.einsteinsworkshopedu.features.boxes.Box;
import me.pietelite.einsteinsworkshopedu.features.boxes.BoxCommand;
import me.pietelite.einsteinsworkshopedu.features.boxes.BoxManager;
import me.pietelite.einsteinsworkshopedu.features.boxes.PlayerLocationManager;
import me.pietelite.einsteinsworkshopedu.features.homes.HomeCommand;
import me.pietelite.einsteinsworkshopedu.features.homes.HomeManager;
import me.pietelite.einsteinsworkshopedu.listeners.*;
import me.pietelite.einsteinsworkshopedu.tools.EweduElementManager;
import org.slf4j.Logger;
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
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.TargetInventoryEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.google.inject.Inject;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.SpongeCommandIssuer;
import co.aikar.commands.SpongeCommandManager;
import me.pietelite.einsteinsworkshopedu.features.assignments.Assignment;
import me.pietelite.einsteinsworkshopedu.features.assignments.AssignmentCommand;
import me.pietelite.einsteinsworkshopedu.features.assignments.AssignmentManager;
import me.pietelite.einsteinsworkshopedu.features.freeze.FreezeCommand;
import me.pietelite.einsteinsworkshopedu.features.freeze.FreezeManager;
import me.pietelite.einsteinsworkshopedu.features.freeze.UnfreezeCommand;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

/**
 * The main class for the EDU plugin for Einstein's Workshop.
 * This plugin is made exclusively for Einstein's Workshop
 * Do not use this plugin without explicit approval from a member of staff from
 * Einstein's Workshop.
 * <p>
 * <b>Permissions</b>
 * <p>
 * <li><i>einsteinsworkshop.student</i>: Allows the use of the student-level abilities</li>
 * <li><i>einsteinsworkshop.instructor</i>: Allows the use of all instructor-level abilities</li>
 * <li><i>einsteinsworkshop.immunity</i>: Makes the player immune to student-altering effects</i>
 */
@Plugin(id = ID,
		name = "EinsteinsWorkshopEDU",
		version = VERSION,
		description = "Education Administrative Tool")
public class EweduPlugin implements PluginContainer {
	
	static final String VERSION = "1.0";
	static final String ID = "einsteinsworkshopedu";
	
	private static final String LOG_IN_MESSAGE_FILE_NAME = "log_in_message.txt";
	
	private static final String DATA_FOLDER_NAME = "einsteinsworkshop";

	/** General logger. From Sponge API. */
	@Inject
    private Logger logger;

	/** Location of the default configuration file for this plugin. From Sponge API. */
	@Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfig;

	/** Configuration manager of the configuration file. From Sponge API. */
	@Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    /** The root node of the configuration file, using the configuration manager. */
    private ConfigurationNode rootNode;
    
    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDirectory;
    
    @Inject
    private PluginContainer container;
    
    private SpongeCommandManager commandManager;

    private FeatureManager featureManager;

    private FreezeManager freezeManager;

    private List<EweduElementManager> elementManagers = new LinkedList<>();
    private BoxManager boxManager;
    private AssignmentManager assignmentManager;
    private HomeManager homeManager;

    private PlayerLocationManager playerLocationManager;
    
    private List<String> loginMessage;

    private static List<String> assignmentTypes;

	/**
	 * Run initialization sequence before the game starts.
	 * All classes that other classes depend on must be initialized here.
	 * @param event the event run before the game starts
	 */
    @Listener
    public void onInitialize(GameInitializationEvent event) {
        logger.info("Initializing EinsteinsWorkshopEdu...");

        featureManager = new FeatureManager(this);
        freezeManager = new FreezeManager(this);
        assignmentManager = new AssignmentManager(this);
        boxManager = new BoxManager(this);
        playerLocationManager = new PlayerLocationManager(this);
        
        loginMessage = readLoginMessageFile(loadLoginMessageFile());
        
        // Load the config from the Sponge API and set the specific node values.
        initializeConfig();
        loadConfig();
        
        // Classes which other classes depend on must be initialized here. 
        
        // Register all the listeners with Sponge
        registerListeners();
        
        // Register all the commands with Sponge
        registerCommands(); 
    }

    @Listener
	public void onStarted(GameStartedServerEvent event) {
		homeManager = new HomeManager(this);
	}

	private void initializeConfig() {
    	if (!defaultConfig.toFile().exists()) {
            logger.info("Generating New Configuration File...");
            try {
                rootNode = configManager.load();
                ConfigurationNode featureNode = rootNode.getNode("features");
				for (FeatureManager.Feature feature : featureManager.getFeatures()) {
					featureNode.getNode(feature.name).getNode("enabled").setValue(true);
				}
                featureNode.getNode("assignments").getNode("types").setValue(Assignment.DEFAULT_ASSIGNMENT_TYPES);
				featureNode.getNode("boxes").getNode("wand_item").setValue(Box.DEFAULT_BOX_WAND.getName());
                configManager.save(rootNode);
                logger.info("New Configuration File created successfully!");
            } catch (IOException e) {
                logger.warn("Exception while reading configuration", e);
            }
        }
	}
	
	private void loadConfig() {
        try {
        	rootNode = configManager.load();
            ConfigurationNode featureNode = rootNode.getNode("features");
			for (FeatureManager.Feature feature : featureManager.getFeatures()) {
				feature.isEnabled = featureNode.getNode(feature.name).getNode("enabled").getBoolean();
			}
			assignmentTypes = featureNode.getNode("assignments").getNode("types").getList((object) -> {
				if (object == null) {
					return null;
				} else {
					return object.toString();}
				});
			this.getBoxManager().setWandItemName(featureNode.getNode("boxes").getNode("wand_item").getString());
			getLogger().info("List of Valid Assignment Types: " + assignmentTypes.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	@SuppressWarnings("deprecation")
	private void registerCommands() {
    	commandManager = new SpongeCommandManager(this.container);
    	commandManager.enableUnstableAPI("help");
    	commandManager.createRootCommand("einsteinsworkshop");
    	commandManager.registerCommand(new EinsteinsWorkshopCommand(this));
    	commandManager.registerCommand(new FreezeCommand(this));
    	commandManager.registerCommand(new UnfreezeCommand(this));
    	commandManager.registerCommand(new AssignmentCommand(this));
    	commandManager.registerCommand(new BoxCommand(this));
    	commandManager.registerCommand(new HomeCommand(this));
    	registerConditions();
    	registerCompletions();
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
		Sponge.getEventManager().registerListener(this, TargetEntityEvent.class, Order.LAST, new TargetEntityEventListener(this));
		Sponge.getEventManager().registerListener(this, InteractEvent.class, Order.LAST, new InteractEventListener(this));
		Sponge.getEventManager().registerListener(this, TargetInventoryEvent.class, Order.LAST, new TargetInventoryEventListener(this));
		Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Join.class, Order.LAST, new LoginEventListener(this));
		Sponge.getEventManager().registerListener(this, MessageChannelEvent.Chat.class, Order.LAST, new ChatListener(this));
		Sponge.getEventManager().registerListener(this, ChangeBlockEvent.class, Order.FIRST, new ChangeBlockListener(this));
	}	

	/**
     * To be run when the plugin reloads
     * @param event The GameReloadEvent
     */
    @Listener
    public void onReload(GameReloadEvent event) {
        logger.info("Reloading EinsteinsWorkshopEDU config data");
        try {
            rootNode = configManager.load();
        } catch (IOException e) {
            logger.warn("Exception while reading configuration", e);
        }
        logger.info("EinsteinsWorkshopEDU config data reloaded!");
    }

    // TODO: Make a feature object for the login message
    private File loadLoginMessageFile() {
    	logger.info("Loading Login Message File");
    	
    	if (configDirectory.mkdir()) getLogger().info("EinsteinsWorkshopEDU Configuration Directory Created");
    	
    	// Get the file
    	Path filePath = Paths.get(configDirectory.getPath(), LOG_IN_MESSAGE_FILE_NAME);
        
        if (Files.notExists(filePath)) {
        	getLogger().info("File doesn't exist yet! Trying to create as '" + filePath + "'");
            getAsset("default_log_in_message.txt").ifPresent(asset -> {
				try {
					asset.copyToFile(filePath, false);
					getLogger().info("'" + LOG_IN_MESSAGE_FILE_NAME + "' created successfully.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
        }
		return filePath.toFile();
    }
    
    private List<String> readLoginMessageFile(File file) {
        try {
            Scanner scanner = new Scanner(file);
            List<String> lines = new LinkedList<String>();
            while (scanner.hasNext()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
            return lines;
        } catch (Exception e) {
            logger.warn("Exception while loading", e);
            return new LinkedList<>();
        }
    }

    public SpongeCommandManager getCommandManager() {
    	return commandManager;
	}

    public FeatureManager getFeatureManager() {
    	return featureManager;
	}

    public FreezeManager getFreezeManager() {
    	return freezeManager;
    }
    
    public List<EweduElementManager> getElementManagers() {
    	return elementManagers;
	}

	public BoxManager getBoxManager() {
    	return boxManager;
	}

	public AssignmentManager getAssignmentManager() {
    	return assignmentManager;
	}

	public HomeManager getHomeManager() {
    	return homeManager;
	}

	public PlayerLocationManager getPlayerLocationManager() {
		return playerLocationManager;
	}

    public Logger getLogger() {
    	return logger;
    }
    
    public List<String> getLoginMessage() {
    	return loginMessage;
    }

	@Override
	public String getId() {
		return "ewedu";
	}

	public File getDataDirectory() {
		return new File(configDirectory.getParentFile().getParentFile().getPath() + "/" + DATA_FOLDER_NAME);
	}
	
	public static List<String> getAssignmentTypes() {
		return assignmentTypes;
	};

}