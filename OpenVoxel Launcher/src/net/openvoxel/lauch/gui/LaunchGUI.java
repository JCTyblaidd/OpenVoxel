package net.openvoxel.lauch.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by James on 18/09/2016.
 *
 * GUI Handle
 */
public class LaunchGUI extends Application{

	private TabPane tabPane;
	private Tab tabLaunch, tabConfig, tabConsole;
	private StackPane ROOT;
	public RichFormattedArea console;

	public void start(Stage app) throws Exception {
		app.setTitle("Open Voxel Launcher");
		app.setMinHeight(200);
		app.setMinWidth(300);
		app.setHeight(480);
		app.setWidth(720);
		tabLaunch = new Tab("Launcher");
		tabConfig = new Tab("Config Editor");
		tabConsole = new Tab("Console");
		tabConsole.setClosable(false);
		tabConfig.setClosable(false);
		tabLaunch.setClosable(false);
		tabPane = new TabPane(tabLaunch,tabConfig,tabConsole);

		console = new RichFormattedArea();
		tabConsole.setContent(console);

		ROOT = new StackPane();
		ROOT.getChildren().add(tabPane);
		app.setScene(new Scene(ROOT));
		app.show();
	}

	@Override
	public void stop() throws Exception {
		System.exit(-1);
	}
}
