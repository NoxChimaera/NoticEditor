package com.temporaryteam.noticeditor.controller;

import com.temporaryteam.noticeditor.controller.notifier.Notifier;
import com.temporaryteam.noticeditor.io.*;
import com.temporaryteam.noticeditor.io.format.Format;
import com.temporaryteam.noticeditor.io.format.FormatException;
import com.temporaryteam.noticeditor.io.format.FormatService;
import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.*;

import com.temporaryteam.noticeditor.io.importers.FileImporter;
import com.temporaryteam.noticeditor.model.*;
import com.temporaryteam.noticeditor.view.NotificationBox;
import com.temporaryteam.noticeditor.view.selector.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Main window controller
 * View: fxml/Main.fxml
 */
public class NoticeController {

	private static final Logger LOGGER = Logger.getLogger(NoticeController.class.getName());

	@FXML
	private ResourceBundle resources;
	
	@FXML
	private VBox noticeTreeView;
	
	@FXML
	private SplitPane noticeView;

	@FXML
	private CheckMenuItem wordWrapItem;

	@FXML
	private Menu recentFilesMenu, previewStyleMenu;
	
	@FXML
	private VBox notificationBox;
	
	@FXML
	private Label notificationLabel;
	
	@FXML
	private NoticeTreeViewController noticeTreeViewController;

	@FXML
	private NoticeViewController noticeViewController;
	
	
	private static NoticeController instance;
	private Stage primaryStage;
	
//	private File fileSaved;
	private IO lastDatasource;

	public NoticeController() {
		instance = this;
	}
	
	/**
	 * Sets primary stage
	 * 
	 * @param aPrimaryStage Primary stage
	 */
	public void setPrimaryStage(Stage aPrimaryStage) {
		primaryStage = aPrimaryStage;
	}
	
	/**
	 * Returns instance of this class
	 * 
	 * @return Instance
	 */
	public static NoticeController getController() {
		return instance;
	}
	
	/**
	 * Returns controller of notice editor (and preview)
	 * 
	 * @return Controller of NoticeView
	 */
	public static NoticeViewController getNoticeViewController() {
		return instance.noticeViewController;
	}

	/**
	 * Returns controller of notice tree (left panel, without search and status)
	 * @return 
	 */
	public static NoticeTreeViewController getNoticeTreeViewController() {
		return instance.noticeTreeViewController;
	}
	
	/**
	 * Initializes the controller class.
	 */
	@FXML
	private void initialize() {
		Notifier.register(new NotificationBox(notificationBox, notificationLabel));
		// Restore initial directory
		File initialDirectory = new File(Prefs.getLastDirectory());
		if (initialDirectory.isDirectory() && initialDirectory.exists()) {
			SelectorDialogService.setInitialDirectory(initialDirectory);
		}
		rebuildRecentFilesMenu();
				
		fillPreviewStyleMenu();

		noticeViewController.getEditor().wrapTextProperty().bind(wordWrapItem.selectedProperty());
		noticeTreeViewController.rebuildTree(resources.getString("help"));
	}

	// Set preview styles menu items
	private void fillPreviewStyleMenu() {
		ToggleGroup previewStyleGroup = new ToggleGroup();
		for (PreviewStyles style : PreviewStyles.values()) {
			final String cssPath = style.getCssPath();
			RadioMenuItem item = new RadioMenuItem(style.getName());
			item.setUserData(cssPath);
			item.setToggleGroup(previewStyleGroup);
			if (cssPath == null) {
				item.setSelected(true);
			}
			item.setOnAction(noticeViewController.onPreviewStyleChange);
			previewStyleMenu.getItems().add(item);
		}
	}
	
	private void rebuildRecentFilesMenu() {
		recentFilesMenu.getItems().clear();
		Prefs.getRecentFiles().stream()
				.distinct()
				.map(File::new)
				.filter(File::exists)
				.filter(File::isFile)
				.forEach(file -> {
					MenuItem item = new MenuItem(file.getAbsolutePath());
					item.setOnAction(e -> {
						FileIO io = new FileIO(file);
						lastDatasource = io;
						openDocument(io);
					});
					recentFilesMenu.getItems().add(item);
				});
		recentFilesMenu.setDisable(recentFilesMenu.getItems().isEmpty());
	}

	@FXML
	private void handleNew(ActionEvent event) {
		noticeTreeViewController.rebuildTree(resources.getString("help"));
		lastDatasource = null;
		NoticeStatusList.restore();
	}

	@FXML
	private void handleOpen(ActionEvent event) {
		FileIO io = (FileIO) SelectorDialogService.get(FileLoaderDialog.class)
			.filter(FileSelectorDialog.SUPPORTED, FileSelectorDialog.ALL)
			.show("Open notice")
			.io();
		
		if (io.isAvailable()) {
			openDocument(io);
			Prefs.addToRecentFiles(io.getPath());
			rebuildRecentFilesMenu();
		}
	}
	
	private void openDocument(IO io) {
		try {
			NoticeTree tree = DocumentFormat.open(io);
			noticeTreeViewController.rebuildTree(tree);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, null, e);
			Notifier.error("Unable to open " + io.getDatasourceName());
		}
	}

	@FXML
	private void handleSave(ActionEvent event) {
		if (lastDatasource == null) {
			handleSaveAs(event);
		} else {
			saveDocument(lastDatasource);
		}
	}

	@FXML
	private void handleSaveAs(ActionEvent event) {
		FileIO io = (FileIO) SelectorDialogService.get(FileSaverDialog.class)
			.filter(FileSelectorDialog.ZIP, FileSelectorDialog.JSON)
			.show("Save notice")
			.io();

		if (io.isAvailable()) {
			saveDocument(io);
		}
	}

	private void saveDocument(IO io) {
		try {
			Format fmt = FormatService.get("json");
			if (io.getDatasourceName().toLowerCase().endsWith(".zip")) {
				fmt = FormatService.get("zip");
			}
			
			NoticeTree tree = noticeTreeViewController.getNoticeTree();
			DocumentFormat.save(io, tree, fmt);
			Notifier.success("Successfully saved!");
		} catch (FormatException | IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			Notifier.error("Failed...");
		}
	}
	
	// TODO: change File to FileIO
	@FXML
	private void handleExportHtml(ActionEvent event) {
		File destDir = SelectorDialogService.get(DirectorySelectorDialog.class)
			.show("Select directory to save HTML files")
			.result();
		if (destDir == null) {
			return;
		}

		try {
			FormatService.HTML.setProcessor(noticeViewController.processor);
			FormatService.HTML.export(destDir, noticeTreeViewController.getNoticeTree());
			Notifier.success("Export success!");
		} catch (ExportException e) {
			LOGGER.log(Level.SEVERE, null, e);
			Notifier.error("Export failed!");
		}
	}

	@FXML
	private void handleExit(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	private void handleSwitchOrientation(ActionEvent event) {
		noticeView.setOrientation(noticeView.getOrientation() == Orientation.HORIZONTAL
				? Orientation.VERTICAL : Orientation.HORIZONTAL);
	}

	@FXML
	private void handleAbout(ActionEvent event) {
		Notifier.message("NoticEditor\n==========\n\nhttps://github.com/TemporaryTeam/NoticEditor");
	}
	
	@FXML
	private void handleImportUrl(ActionEvent event) {
		try {
			final ResourceBundle resource = ResourceBundle.getBundle("resources.i18n.WebImport", Locale.getDefault());
			
			Stage stage = new Stage();
			stage.setTitle(resource.getString("import"));
			stage.initOwner(primaryStage);
			stage.initModality(Modality.WINDOW_MODAL);
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WebImport.fxml"), resource);
			Scene scene = new Scene(loader.load());
			stage.setScene(scene);
			WebImportController controller = (WebImportController) loader.getController();
			controller.setImportCallback((html, ex) -> {
				if (ex != null) {
					Notifier.error(ex.toString());
				} else if (html != null) {
					noticeViewController.getEditor().setText(html);
				}
				stage.close();
			});
			stage.show();
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, null, e);
		}
	}

	// TODO: change File to FileIO
	@FXML
	private void handleImportFile(ActionEvent event) {
		File file = SelectorDialogService.get(FileLoaderDialog.class)
			.filter(FileSelectorDialog.SUPPORTED, FileSelectorDialog.ALL)
			.show("Import file")
			.result();
		if (file == null) return;

		FileImporter.content().importFrom(file, null, (text, ex) -> {
			if (ex != null) {
				Notifier.error(ex.toString());
			} else if (text != null) {
				noticeViewController.getEditor().setText(text);
			}
		});
	}

	public void onExit(WindowEvent we) {
		File lastDir = SelectorDialogService.getLastDirectory();
		if (lastDir == null) {
			return;
		}
		
		Prefs.setLastDirectory(lastDir.getAbsolutePath());
	}
}
