package syam.UsefulTNT;

import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import uk.co.oliwali.HawkEye.entry.DataEntry;
import uk.co.oliwali.HawkEye.util.HawkEyeAPI;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class UsefulTNT extends JavaPlugin {
	// Logger
	public final static Logger log = Logger.getLogger("Minecraft");
	public final static String logPrefix = "[UsefulTNT] ";
	public final static String msgPrefix = "&c[UsefulTNT] &f";

	// Listener
	private final TNTListener tntListener = new TNTListener(this);

	// Public classes
	public static PrimedTNTManager primedManager;

	// Instance
	private static UsefulTNT instance;

	// Hookup plugins
	public WorldGuardPlugin wgPlugin;
	public static boolean usingHawkEye = false;
	public static boolean usingWorldGuard = false;

	/**
	 * プラグイン起動処理
	 */
	public void onEnable(){
		instance = this;
		PluginManager pm = getServer().getPluginManager();

		// プラグインフック

		// HawkEyeAPIがあるため、HawkEyeの有無だけを確認すれば良い
		Plugin p = pm.getPlugin("HawkEye");
		if (p == null){
			log.warning(logPrefix+"Cannot find HawkEye! Disabling plugin.");
			//getPluginLoader().disablePlugin(this);
			//return;
		}else{
			usingHawkEye = true; // フラグ
			log.info(logPrefix+"Hooked to Hawkeye!");
		}

		// WorldGuard
		p = pm.getPlugin("WorldGuard");
		if (p == null || !(p instanceof WorldGuardPlugin)){
			log.warning(logPrefix+"Cannot find WorldGuard! Disabling plugin.");
			getPluginLoader().disablePlugin(this);
			return;
		}else{
			usingWorldGuard = true;
			this.wgPlugin = ((WorldGuardPlugin)p);
			log.info(logPrefix+"Hooked to WorldGuard!");
		}


		// Listner登録
		pm.registerEvents(tntListener, this);

		// TNTマネージャを初期化
		primedManager = new PrimedTNTManager(this);

		// メッセージ表示
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info("["+pdfFile.getName()+"] version "+pdfFile.getVersion()+" is Enabled!");
	}

	/**
	 * プラグイン停止処理
	 */
	public void onDisable(){
		// メッセージ表示
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info("["+pdfFile.getName()+"] version "+pdfFile.getVersion()+" is Disabled!");
	}

	public boolean logToHawkeye(DataEntry entry){
		if (usingHawkEye){
			return HawkEyeAPI.addEntry(this, entry);
		}else{
			return false;
		}
	}

	/**
	 * インスタンスを返す
	 * @return シングルトンインスタンス or null
	 */
	public static UsefulTNT getInstance(){
		return instance;
	}
}
