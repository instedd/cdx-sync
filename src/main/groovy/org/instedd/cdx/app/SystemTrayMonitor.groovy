package org.instedd.cdx.app

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.net.URL;

import javax.swing.JOptionPane;

import org.instedd.sync4j.app.RSyncApplication;

class SystemTrayMonitor  extends org.instedd.sync4j.app.SystemTrayMonitor {

  String dbPath;

  public SystemTrayMonitor(String tooltip, URL imageUrl, String dbPath) {
    super(tooltip, imageUrl);
    this.dbPath = dbPath;
  }

  @Override
  protected void configureMenu(RSyncApplication application, PopupMenu menu) {
    super.configureMenu(application, menu);
    MenuItem menuItem = new MenuItem("Reconfigure");
    menuItem.addActionListener({
      def result = JOptionPane.showConfirmDialog(null,
          "This will erease your configurations. Would you want to proceed?", "Erase configurations?", JOptionPane.YES_NO_OPTION)
      if (result == JOptionPane.YES_OPTION) {
        new File(dbPath).deleteOnExit();
        JOptionPane.showMessageDialog(null, "Changes will take effect after restart.")
      }
    });
    menu.add(menuItem);
  }
}