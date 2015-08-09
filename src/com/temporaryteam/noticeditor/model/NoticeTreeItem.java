package com.temporaryteam.noticeditor.model;

import java.util.ArrayList;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model representation of notice. Contains notice data or branch data
 *
 * @author naik, setser, annimon, kalter
 */
public class NoticeTreeItem extends TreeItem<String> {

	public static final String KEY_TITLE = "title";
	public static final String KEY_CONTENT = "content";
	public static final String KEY_CHILDREN = "children";

	public static final int STATUS_NORMAL = 1;
	public static final int STATUS_IMPORTANT = 2;

	private String title;
	private ObservableList<TreeItem<String>> children;
	private String content;
	private int status;

	/**
	 * Create branch node on tree.
	 *
	 * @param title
	 */
	public NoticeTreeItem(String title) {
		this(title, null);
	}

	/**
	 * Create leaf node on tree.
	 *
	 * @param title
	 * @param content
	 */
	public NoticeTreeItem(String title, String content) {
		super(title);
		this.title = title;
		this.content = content;
		children = getChildren();
		status = STATUS_NORMAL;
	}

	public NoticeTreeItem(JSONObject json) throws JSONException {
		this(json.getString(KEY_TITLE), json.optString(KEY_CONTENT, null));
		JSONArray arr = json.getJSONArray(KEY_CHILDREN);
		for (int i = 0; i < arr.length(); i++) {
			children.add(new NoticeTreeItem(arr.getJSONObject(i)));
		}
	}
	
	public void addChild(NoticeTreeItem item) {
		children.add(item);
	}

	@Override
	public boolean isLeaf() {
		return content != null;
	}

	/**
	 * @return true if content == null
	 */
	public boolean isBranch() {
		return content == null;
	}

	/**
	 * @return notice content or null if its a branch
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Content will be changed only when is a leaf node.
	 *
	 * @param content
	 */
	public void changeContent(String content) {
		if (isLeaf()) {
			this.content = content;
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		setValue(title);
		this.title = title;
	}

	public void setStatus(int status) {
		this.status = status;
		Event.fireEvent(this, new TreeModificationEvent(childrenModificationEvent(), this));
	}

	public int getStatus() {
		return status;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put(KEY_TITLE, title);
		json.putOpt(KEY_CONTENT, content);
		ArrayList list = new ArrayList();
		for (TreeItem<String> treeItem : children) {
			NoticeTreeItem child = (NoticeTreeItem) treeItem;
			list.add(child.toJson());
		}
		json.put(KEY_CHILDREN, new JSONArray(list));
		return json;
	}

}
