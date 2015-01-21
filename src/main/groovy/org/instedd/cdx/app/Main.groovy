package org.instedd.cdx.app

import static org.instedd.sync4j.util.Exceptions.interruptable

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Properties
import java.util.Scanner

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.instedd.sync4j.Settings
import org.instedd.sync4j.app.ConsoleMonitor;
import org.instedd.sync4j.app.RSyncApplication
import org.instedd.sync4j.credentials.Credentials;
import org.instedd.sync4j.settings.MapDBSettingsStore;
import org.instedd.sync4j.watcher.RsyncWatchListener.SyncMode

public class Main {
  public static void main(String[] args) {
    if (args.length != 1) {
      println("Usage: cdxsync <properties filename>")
      System.exit(1)
    }
    def propertiesFilename = args[0]
    def properties = properties(propertiesFilename)

    def appName = properties['app.name']
    def appIcon = Main.getResource(properties['app.icon'])
    def appMode = SyncMode.valueOf(properties['app.mode'].toUpperCase())
    def dbPath = properties['app.dbPath']

    def appSettings = [
      authServerUrl: properties['app.server.url'],
      remoteKey: properties['app.remote.key'],
      knownHostsFilePath: properties['app.know.hosts.file.path']]

    def settings = readOrHandshakeSettings(dbPath, appSettings)

    startApplication(settings, appMode, appName, appIcon, dbPath)

    printf("\n\n** Now go and create or edit some files on %s **\n\n", settings.localOutboxDir)
  }

  static startApplication(settings, SyncMode appMode, appName, URL appIcon, dbPath) {
    def app = new RSyncApplication(settings, appMode)
    app.start(new SystemTrayMonitor(appName, appIcon, dbPath), new ConsoleMonitor())
  }

  static readOrHandshakeSettings(dbPath, appSettings) {
    def db = MapDBSettingsStore.fromMapDB(dbPath)
    if(!db.settings) {
      db.settings = handshakeSettings(appSettings)
      JOptionPane.showMessageDialog(null, "Device is now activated");
    }
    db.settings
  }

  static handshakeSettings(appSettings) {
    def serverSettings
    def userSettings
    while (true) {
      userSettings = UserSettingsPrompt.promptForUserSettings()
      def credentials = new Credentials(new File(appSettings.remoteKey))
      def authServer = new SyncAuthServer(userSettings.authToken, appSettings.authServerUrl)
      try {
        credentials.ensure()
        serverSettings = authServer.authenticate(credentials.publicKey)
        break
      } catch(Exception e) {
        confirmRetryOrExit(e)
      }
    }
    merge(appSettings, userSettings, serverSettings)
  }

  static confirmRetryOrExit(Exception e) {
    def result = JOptionPane.showConfirmDialog(null, "${e.message}. Try again?", "Try again?", JOptionPane.YES_NO_OPTION)
    if (result == JOptionPane.NO_OPTION) {
      System.exit(1);
    }
  }

  static merge(appSettings, userSettings, serverSettings) {
    new Settings(
      remoteHost: serverSettings.host,
      remotePort: serverSettings.port,
      remoteUser: serverSettings.user,
      remoteInboxDir: serverSettings.inbox_dir,
      remoteOutboxDir: serverSettings.outbox_dir,

      remoteKey: appSettings.remoteKey,
      knownHostsFilePath: appSettings.knownHostsFilePath,

      localInboxDir: userSettings.localInboxDir,
      localOutboxDir: userSettings.localOutboxDir)
  }

  static properties(String propertiesFilename) {
    def properties = new Properties()
    new File(propertiesFilename).withInputStream { properties.load(it) }
    properties
  }
}
