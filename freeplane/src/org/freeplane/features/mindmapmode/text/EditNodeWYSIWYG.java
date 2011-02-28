/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.mindmapmode.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.frame.ViewController;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.common.map.NodeModel;
import org.freeplane.features.mindmapmode.ortho.SpellCheckerController;

import com.lightdev.app.shtm.SHTMLEditorPane;
import com.lightdev.app.shtm.SHTMLPanel;

/**
 * @author Daniel Polansky
 */
public class EditNodeWYSIWYG extends EditNodeBase {
	private static class HTMLDialog extends EditDialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private SHTMLPanel htmlEditorPanel;
		private JButton splitButton;

		HTMLDialog(final EditNodeBase base, final String title, String purpose, final Frame frame) throws Exception {
			super(base, title, frame);
			createEditorPanel(purpose);
			getContentPane().add(htmlEditorPanel, BorderLayout.CENTER);
			UITools.addEscapeActionToDialog(this, new CancelAction());
			final JButton okButton = new JButton();
			final JButton cancelButton = new JButton();
			splitButton = new JButton();
			MenuBuilder.setLabelAndMnemonic(okButton, TextUtils.getText("ok"));
			MenuBuilder.setLabelAndMnemonic(cancelButton, TextUtils.getText("cancel"));
			MenuBuilder.setLabelAndMnemonic(splitButton, TextUtils.getText("split"));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					submit();
				}
			});
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					cancel();
				}
			});
			splitButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					split();
				}
			});
			UITools.addKeyActionToDialog(this, new SubmitAction(), "alt ENTER", "submit");
			final JPanel buttonPane = new JPanel();
			buttonPane.add(okButton);
			buttonPane.add(cancelButton);
			buttonPane.add(splitButton);
			buttonPane.setMaximumSize(new Dimension(1000, 20));
			if (ResourceController.getResourceController().getBooleanProperty("el__buttons_above")) {
				getContentPane().add(buttonPane, BorderLayout.NORTH);
			}
			else {
				getContentPane().add(buttonPane, BorderLayout.SOUTH);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see freeplane.view.mindmapview.EditNodeBase.Dialog#close()
		 */
		@Override
		protected void cancel() {
			final StyleSheet styleSheet = htmlEditorPanel.getDocument().getStyleSheet();
			styleSheet.removeStyle("p");
			styleSheet.removeStyle("BODY");
			getBase().getEditControl().cancel();
			super.cancel();
		}

		private SHTMLPanel createEditorPanel(String purpose) throws Exception {
			if (htmlEditorPanel == null) {
				htmlEditorPanel = MTextController.getController().createSHTMLPanel(purpose);
				final SHTMLEditorPane editorPane = (SHTMLEditorPane) htmlEditorPanel.getEditorPane();
				final SpellCheckerController spellCheckerController = SpellCheckerController.getController();
				spellCheckerController.enableAutoSpell(editorPane, true);
				spellCheckerController.addSpellCheckerMenu(editorPane.getPopup());
				spellCheckerController.enableShortKey(editorPane, true);
			}
			return htmlEditorPanel;
		}

		/**
		 * @return Returns the htmlEditorPanel.
		 */
		public SHTMLPanel getHtmlEditorPanel() {
			return htmlEditorPanel;
		}

		@Override
		public Component getMostRecentFocusOwner() {
			if (isFocused()) {
				return getFocusOwner();
			}
			else {
				return htmlEditorPanel.getMostRecentFocusOwner();
			}
		}

		@Override
		protected boolean isChanged() {
			return htmlEditorPanel.needsSaving();
		}

		/*
		 * (non-Javadoc)
		 * @see freeplane.view.mindmapview.EditNodeBase.Dialog#split()
		 */
		@Override
		protected void split() {
			final StyleSheet styleSheet = htmlEditorPanel.getDocument().getStyleSheet();
			styleSheet.removeStyle("p");
			styleSheet.removeStyle("body");
			getBase().getEditControl().split(HtmlUtils.unescapeHTMLUnicodeEntity(htmlEditorPanel.getDocumentText()),
			    htmlEditorPanel.getCaretPosition());
			super.split();
		}

		/*
		 * (non-Javadoc)
		 * @see freeplane.view.mindmapview.EditNodeBase.Dialog#close()
		 */
		@Override
		protected void submit() {
			htmlEditorPanel.getDocument().getStyleSheet().removeStyle("p");
			htmlEditorPanel.getDocument().getStyleSheet().removeStyle("body");
			if (htmlEditorPanel.needsSaving()) {
				getBase().getEditControl().ok(HtmlUtils.unescapeHTMLUnicodeEntity(htmlEditorPanel.getDocumentText()));
			}
			else {
				getBase().getEditControl().cancel();
			}
			super.submit();
		}

		public void setSplitEnabled(boolean enableSplit) {
			splitButton.setEnabled(enableSplit);
	        splitButton.setVisible(enableSplit);
        }
	}

	final private KeyEvent firstEvent;
	final private boolean enableSplit;
	final private String purpose;

	public EditNodeWYSIWYG(String purpose, final NodeModel node, final String text, final KeyEvent firstEvent, final IEditControl editControl, boolean enableSplit) {
		super(node, text, editControl);
		this.firstEvent = firstEvent;
		this.enableSplit = enableSplit;
		this.purpose = purpose;
	}

	public void show(final JFrame frame) {
		try {
			HTMLDialog htmlEditorWindow = createHtmlEditor(frame);
			htmlEditorWindow.setBase(this);
			final String title;
			title = TextUtils.getText(purpose);
			htmlEditorWindow.setTitle(title);
			htmlEditorWindow.setSplitEnabled(enableSplit);
			final SHTMLPanel htmlEditorPanel = (htmlEditorWindow).getHtmlEditorPanel();
			final ViewController viewController = Controller.getCurrentModeController().getController().getViewController();
			final Font font = viewController.getFont(node);
			final StringBuilder ruleBuilder = new StringBuilder(100);
			ruleBuilder.append("body {");
			ruleBuilder.append("font-family: ").append(font.getFamily()).append(";");
			ruleBuilder.append("font-size: ").append(font.getSize()).append("pt;");
			if (font.isItalic()) {
				ruleBuilder.append("font-style: italic; ");
			}
			if (font.isBold()) {
				ruleBuilder.append("font-weight: bold; ");
			}
			final Color nodeTextColor = viewController.getTextColor(node);
			ruleBuilder.append("color: ").append(ColorUtils.colorToString(nodeTextColor)).append(";");
			ruleBuilder.append("}\n");
			ruleBuilder.append("p {margin-top:0;}\n");
			final HTMLDocument document = htmlEditorPanel.getDocument();
			final JEditorPane editorPane = htmlEditorPanel.getEditorPane();
			editorPane.setForeground(nodeTextColor);
			editorPane.setBackground(getBackground());
			editorPane.setCaretColor(nodeTextColor);
			final StyleSheet styleSheet = document.getStyleSheet();
			styleSheet.removeStyle("p");
			styleSheet.removeStyle("body");
			styleSheet.addRule(ruleBuilder.toString());
			final URL url = node.getMap().getURL();
			if (url != null) {
				document.setBase(url);
			}
			else {
				document.setBase(new URL("file: "));
			}
			int preferredHeight = (int) (viewController.getComponent(node).getHeight() * 1.2);
			preferredHeight = Math.max(preferredHeight, Integer.parseInt(ResourceController.getResourceController()
			    .getProperty("el__min_default_window_height")));
			preferredHeight = Math.min(preferredHeight, Integer.parseInt(ResourceController.getResourceController()
			    .getProperty("el__max_default_window_height")));
			int preferredWidth = (int) (viewController.getComponent(node).getWidth() * 1.2);
			preferredWidth = Math.max(preferredWidth, Integer.parseInt(ResourceController.getResourceController()
			    .getProperty("el__min_default_window_width")));
			preferredWidth = Math.min(preferredWidth, Integer.parseInt(ResourceController.getResourceController()
			    .getProperty("el__max_default_window_width")));
			htmlEditorPanel.setContentPanePreferredSize(new Dimension(preferredWidth, preferredHeight));
			htmlEditorWindow.pack();
			if (ResourceController.getResourceController().getBooleanProperty("el__position_window_below_node")) {
				UITools.setDialogLocationUnder(htmlEditorWindow, node);
			}
			else {
				UITools.setDialogLocationRelativeTo(htmlEditorWindow, node);
			}
			String content = text;
			if (!HtmlUtils.isHtmlNode(content)) {
				content = HtmlUtils.plainToHTML(content);
			}
			htmlEditorPanel.setCurrentDocumentContent(content);
			if (firstEvent instanceof KeyEvent) {
				final KeyEvent firstKeyEvent = firstEvent;
				final JTextComponent currentPane = htmlEditorPanel.getEditorPane();
				if (currentPane == htmlEditorPanel.getMostRecentFocusOwner()) {
					redispatchKeyEvents(currentPane, firstKeyEvent);
				}
			}
			else {
				editorPane.setCaretPosition(htmlEditorPanel.getDocument().getLength());
			}
			htmlEditorPanel.getMostRecentFocusOwner().requestFocus();
			htmlEditorWindow.show();
		}
		catch (final Exception ex) {
			LogUtils.severe("Loading of WYSIWYG HTML editor failed. Use the other editors instead.", ex);
		}
	}

	public HTMLDialog createHtmlEditor(final JFrame frame) throws Exception {
		final JRootPane rootPane = frame.getRootPane();
		HTMLDialog htmlEditorWindow = (HTMLDialog) rootPane.getClientProperty(HTMLDialog.class);
	    if (htmlEditorWindow == null) {
	    	htmlEditorWindow = new HTMLDialog(this, "", "", frame);
	    	rootPane.putClientProperty(HTMLDialog.class, htmlEditorWindow);
	    }
	    return htmlEditorWindow;
    }
}
