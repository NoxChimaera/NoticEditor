package com.temporaryteam.noticeditor.controller;

import com.temporaryteam.noticeditor.controller.notifier.Notifier;
import com.temporaryteam.noticeditor.model.*;
import com.temporaryteam.noticeditor.view.EditNoticeTreeCell;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

public class NoticeTreeViewController implements Initializable {

	@FXML
	private MenuItem addBranchItem, addNoticeItem, deleteItem;

	@FXML
	private SplitMenuButton addItem;

	@FXML
	private Menu addChild, addNeighbour;

	@FXML
	private MenuItem addChildBranch, addChildNotice, addNeighbourBranch, addNeighbourNotice, addWrapperBranch;

	@FXML
	private TextField searchField;

	@FXML
	private Button searchButton, renameButton, deleteButton;

	@FXML
	private MenuButton statusSelectButton;

	@FXML
	private ToolBar manageItemBar;

	@FXML
	private TreeView<NoticeItem> noticeTreeView;

	private NoticeTree noticeTree;
	private NoticeTreeItem currentTreeItem;

	@Override
	public void initialize(URL url, ResourceBundle resources) {
		final EventHandler onStatusChangeAction = new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				final MenuItem newStatus = (MenuItem) event.getSource();
				statusSelectButton.setText(newStatus.getText());
				NoticeTreeItem currentNotice = getCurrentNotice();
				if (currentNotice != null && newStatus.getUserData() != null && currentNotice.isLeaf()) {
					currentNotice.setStatus(((NoticeStatus) newStatus.getUserData()).getCode());
				}
			}
		};
		NoticeStatusList.add(resources.getString("normal"));
		NoticeStatusList.add(resources.getString("important"));
		NoticeStatusList.save();
		for (NoticeStatus status : NoticeStatusList.asObservable()) {
			MenuItem statusItem = new MenuItem(status.getName());
			statusItem.setUserData(status);
			statusItem.setOnAction(onStatusChangeAction);
			statusSelectButton.getItems().add(statusItem);
		}
		statusSelectButton.setGraphic(new Circle(5, Color.AQUAMARINE));
		statusSelectButton.setTooltip(new Tooltip(resources.getString("status")));

		noticeTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		noticeTreeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<NoticeItem>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<NoticeItem>> observable, TreeItem<NoticeItem> oldValue, TreeItem<NoticeItem> newValue) {
				currentTreeItem = (NoticeTreeItem) newValue;
				open();
			}
		});
		noticeTreeView.setCellFactory(new Callback<TreeView<NoticeItem>, TreeCell<NoticeItem>>() {
			@Override
			public TreeCell<NoticeItem> call(TreeView<NoticeItem> p) {
				return new EditNoticeTreeCell();
			}
		});
	}

	public void rebuildTree(String initialNotice) {
		final NoticeTreeItem root = new NoticeTreeItem("Root");
		noticeTree = new NoticeTree(root);
		currentTreeItem = new NoticeTreeItem("Default notice", initialNotice, NoticeItem.STATUS_NORMAL);
		noticeTree.addItem(currentTreeItem, root);
		noticeTreeView.setRoot(root);
		createSearchBinding(root);
		noticeTreeView.getSelectionModel().select(currentTreeItem);
		open();
	}

	public void rebuildTree(NoticeTree noticeTree) {
		this.noticeTree = noticeTree;
		noticeTreeView.setRoot(noticeTree.getRoot());
		createSearchBinding(noticeTree.getRoot());
		currentTreeItem = null;
		open();
	}

	private void createSearchBinding(final NoticeTreeItem root) {
		searchField.clear();
		root.predicateProperty().bind(
				Bindings.createObjectBinding(this::searchTreeItemPredicate, searchField.textProperty()));
	}

	private NoticeTreeItem.Predicate<NoticeItem> searchTreeItemPredicate() {
		if ((searchField.getText() == null) || (searchField.getText().isEmpty())) {
			return null;
		}
		return this::noticeSearch;
	}

	/**
	 * Search by title and content
	 *
	 * @return
	 */
	private boolean noticeSearch(TreeItem<NoticeItem> parent, NoticeItem note) {
		final String searchString = searchField.getText().toLowerCase();

		final String title = note.getTitle().toLowerCase();
		if (title.contains(searchString))
			return true;

		final String content = note.getContent();
		if (content == null || content.isEmpty())
			return false;

		return content.toLowerCase().contains(searchString);
	}

	/**
	 * Open current item in UI. If current item == null or isBranch, interface will be cleared from last data.
	 */
	public void open() {
		boolean isCurrentBranch = currentTreeItem == null || currentTreeItem.isBranch();
		if (isCurrentBranch) {
			NoticeController.getNoticeViewController().getEditor().setDisable(true);
			NoticeController.getNoticeViewController().getEditor().setText("");
			statusSelectButton.setText(null);
			statusSelectButton.setDisable(true);
		} else {
			NoticeController.getNoticeViewController().getEditor().setDisable(false);
			NoticeController.getNoticeViewController().getEditor().setText(currentTreeItem.getContent());
			statusSelectButton.setText(NoticeStatusList.getStatus(currentTreeItem.getStatus()).getName());
			statusSelectButton.setDisable(false);
		}
		manageItemBar.setDisable(currentTreeItem == null);
		addChild.setDisable(!isCurrentBranch);
		addNeighbour.setDisable(currentTreeItem == null);
	}

	@FXML
	private void handleContextMenu(ActionEvent event) {
		Object source = event.getSource();
		if (source == addBranchItem) {
			noticeTree.addItem(new NoticeTreeItem("New branch"), currentTreeItem);
		} else if (source == addNoticeItem) {
			noticeTree.addItem(new NoticeTreeItem("New notice", "", NoticeItem.STATUS_NORMAL), currentTreeItem);
		} else if (source == deleteItem) {
			noticeTree.removeItem(currentTreeItem);
			if (currentTreeItem != null && currentTreeItem.getParent() == null) {
				currentTreeItem = null;
				open();
			}
		}
	}

	@FXML
	private void handleAddItem(ActionEvent event) {
		Object source = event.getSource();
		if (source == addItem) {
			noticeTree.addItem(new NoticeTreeItem("New notice", "", NoticeItem.STATUS_NORMAL), currentTreeItem);
		} else if (source == addChildNotice) {
			noticeTree.addItem(new NoticeTreeItem("New notice", "", NoticeItem.STATUS_NORMAL), currentTreeItem);
		} else if (source == addChildBranch) {
			noticeTree.addItem(new NoticeTreeItem("New branch"), currentTreeItem);
		} else if (source == addNeighbourNotice) {
			noticeTree.addItem(new NoticeTreeItem("New notice", "", NoticeItem.STATUS_NORMAL), (NoticeTreeItem) currentTreeItem.getParent());
		} else if (source == addNeighbourBranch) {
			noticeTree.addItem(new NoticeTreeItem("New branch"), (NoticeTreeItem) currentTreeItem.getParent());
		} else if (source == addWrapperBranch) {
			NoticeTreeItem wrapper = new NoticeTreeItem("New branch");
			NoticeTreeItem item = currentTreeItem;
			NoticeTreeItem parent = (NoticeTreeItem) currentTreeItem.getParent();
			noticeTree.removeItem(currentTreeItem);
			noticeTree.addItem(wrapper, parent);
			noticeTree.addItem(item, wrapper);
			noticeTreeView.getSelectionModel().select(item);
		}
	}

	@FXML
	private void handleSearch(ActionEvent event) {
		Notifier.error("Sorry, search is not implemented yet.");
	}

	@FXML
	private void handleRename(ActionEvent event) {
		noticeTreeView.edit(currentTreeItem);
	}

	@FXML
	private void handleDelete(ActionEvent event) {
		noticeTree.removeItem(currentTreeItem);
		if (currentTreeItem != null && currentTreeItem.getParent() == null) {
			currentTreeItem = null;
			open();
		}
	}

	public NoticeTreeItem getCurrentNotice() {
		return currentTreeItem;
	}

	public NoticeTree getNoticeTree() {
		return noticeTree;
	}
}
