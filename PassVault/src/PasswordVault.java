import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class PasswordVault {

    private static String masterPassword = null;
    private static final HashMap<String, PasswordEntry> vault = new HashMap<>();
    private static final String DATA_FILE = "vault_data.enc";
    private static final String MASTER_PASSWORD_FILE = "master_password.enc";

    private static JFrame mainFrame;
    private static CardLayout cardLayout;
    private static JPanel mainPanel;
    private static JPanel createMasterPasswordPanel;
    private static JPanel loginPanel;
    private static JPanel vaultPanel;
    private static JPanel createPasswordPanel;
    private static JPanel passwordDetailsPanel;

    private static DefaultListModel<String> listModel;
    private static JList<String> passwordList;

    private static JTextField createSiteField;
    private static JTextField createUsernameField;
    private static JTextField createPasswordField;
    private static JTextField editSiteField;
    private static JTextField editUsernameField;
    private static JTextField editPasswordField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mainFrame = new JFrame("Password Vault");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setSize(400, 300);

            mainFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    saveVaultToEncryptedFile();
                    super.windowClosing(e);
                }
            });

            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);

            mainFrame.add(mainPanel);

            createMasterPasswordScreen();
            createLoginScreen();
            createVaultScreen();
            createPasswordCreationScreen();
            createPasswordEditScreen();

            if (loadMasterPassword()) {
                cardLayout.show(mainPanel, "LoginScreen");
            } else {
                cardLayout.show(mainPanel, "CreateMasterPasswordScreen");
            }

            mainFrame.setVisible(true);
        });
    }

    private static void createMasterPasswordScreen() {
        createMasterPasswordPanel = new JPanel(new GridLayout(4, 1));

        JLabel warningLabel = new JLabel("Remember this password, it's important!", SwingConstants.CENTER);

        JLabel label = new JLabel("Create Master Password:", SwingConstants.CENTER);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(250, 30));

        JButton createButton = new JButton("Create Master Password");

        createButton.addActionListener(e -> {
            char[] passwordChars = passwordField.getPassword();
            if (passwordChars.length > 0) {
                masterPassword = new String(passwordChars);
                saveMasterPassword();
                cardLayout.show(mainPanel, "LoginScreen");
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        createMasterPasswordPanel.add(warningLabel);
        createMasterPasswordPanel.add(label);
        createMasterPasswordPanel.add(passwordField);
        createMasterPasswordPanel.add(createButton);

        mainPanel.add(createMasterPasswordPanel, "CreateMasterPasswordScreen");
    }


    private static void createLoginScreen() {
        loginPanel = new JPanel(new GridLayout(3, 1));

        JLabel label = new JLabel("Enter Master Password:", SwingConstants.CENTER);
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            char[] passwordChars = passwordField.getPassword();
            String enteredPassword = new String(passwordChars);
            if (enteredPassword.equals(masterPassword) && SecurityUtil.loadVaultFromEncryptedFile(enteredPassword, vault, DATA_FILE)) {
                cardLayout.show(mainPanel, "VaultScreen");
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Incorrect password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginPanel.add(label);
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);

        mainPanel.add(loginPanel, "LoginScreen");
    }

    private static void createVaultScreen() {
        vaultPanel = new JPanel(new BorderLayout());

        listModel = new DefaultListModel<>();
        passwordList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(passwordList);

        JLabel emptyLabel = new JLabel("No passwords stored", SwingConstants.CENTER);
        vaultPanel.add(emptyLabel, BorderLayout.CENTER);
        emptyLabel.setVisible(listModel.isEmpty());

        JButton createButton = new JButton("Create Password");
        createButton.addActionListener(e -> {
            clearCreatePasswordFields();
            cardLayout.show(mainPanel, "CreatePasswordScreen");
        });

        passwordList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedKey = passwordList.getSelectedValue();
                if (selectedKey != null) {
                    updateDetailsPanel(selectedKey);
                    cardLayout.show(mainPanel, "PasswordDetailsScreen");
                    passwordList.clearSelection();
                }
            }
        });

        vaultPanel.add(scrollPane, BorderLayout.CENTER);
        vaultPanel.add(createButton, BorderLayout.SOUTH);

        mainPanel.add(vaultPanel, "VaultScreen");
    }

    private static void createPasswordCreationScreen() {
        createPasswordPanel = new JPanel(new GridLayout(5, 1));

        JLabel siteLabel = new JLabel("Site:");
        createSiteField = new JTextField();
        JLabel usernameLabel = new JLabel("Username:");
        createUsernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        createPasswordField = new JTextField();

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> savePassword());
        cancelButton.addActionListener(e -> cardLayout.show(mainPanel, "VaultScreen"));

        createPasswordPanel.add(siteLabel);
        createPasswordPanel.add(createSiteField);
        createPasswordPanel.add(usernameLabel);
        createPasswordPanel.add(createUsernameField);
        createPasswordPanel.add(passwordLabel);
        createPasswordPanel.add(createPasswordField);
        createPasswordPanel.add(saveButton);
        createPasswordPanel.add(cancelButton);

        mainPanel.add(createPasswordPanel, "CreatePasswordScreen");
    }

    private static void savePassword() {
        String site = createSiteField.getText();
        String username = createUsernameField.getText();
        String password = createPasswordField.getText();

        if (site.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PasswordEntry entry = new PasswordEntry(site, username, password);
        vault.put(site, entry);
        listModel.addElement(site);

        saveVaultToEncryptedFile();
        cardLayout.show(mainPanel, "VaultScreen");
    }

    private static void createPasswordEditScreen() {
        passwordDetailsPanel = new JPanel(new GridLayout(5, 1));

        JLabel siteLabel = new JLabel("Site:");
        editSiteField = new JTextField();
        JLabel usernameLabel = new JLabel("Username:");
        editUsernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        editPasswordField = new JTextField();

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JButton deleteButton = new JButton("Delete");

        saveButton.addActionListener(e -> saveEditedPassword());
        cancelButton.addActionListener(e -> cardLayout.show(mainPanel, "VaultScreen"));
        deleteButton.addActionListener(e -> deletePassword());

        passwordDetailsPanel.add(siteLabel);
        passwordDetailsPanel.add(editSiteField);
        passwordDetailsPanel.add(usernameLabel);
        passwordDetailsPanel.add(editUsernameField);
        passwordDetailsPanel.add(passwordLabel);
        passwordDetailsPanel.add(editPasswordField);
        passwordDetailsPanel.add(saveButton);
        passwordDetailsPanel.add(cancelButton);
        passwordDetailsPanel.add(deleteButton);

        mainPanel.add(passwordDetailsPanel, "PasswordDetailsScreen");
    }

    private static void updateDetailsPanel(String site) {
        PasswordEntry entry = vault.get(site);
        editSiteField.setText(entry.site);
        editUsernameField.setText(entry.username);
        editPasswordField.setText(entry.password);
    }

    private static void saveEditedPassword() {
        String site = editSiteField.getText();
        String username = editUsernameField.getText();
        String password = editPasswordField.getText();

        if (site.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PasswordEntry updatedEntry = new PasswordEntry(site, username, password);
        vault.put(site, updatedEntry);

        listModel.clear();
        for (String s : vault.keySet()) {
            listModel.addElement(s);
        }

        saveVaultToEncryptedFile();
        cardLayout.show(mainPanel, "VaultScreen");
    }

    private static void deletePassword() {
        String site = editSiteField.getText();
        vault.remove(site);

        listModel.removeElement(site);
        saveVaultToEncryptedFile();
        cardLayout.show(mainPanel, "VaultScreen");
    }

    private static void clearCreatePasswordFields() {
        createSiteField.setText("");
        createUsernameField.setText("");
        createPasswordField.setText("");
    }

    private static void saveVaultToEncryptedFile() {
        if (masterPassword == null) {
            JOptionPane.showMessageDialog(mainFrame, "Master password not set. Unable to save data.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SecurityUtil.saveVaultToEncryptedFile(masterPassword, vault, DATA_FILE);
    }

    private static void saveMasterPassword() {
        try {
            SecurityUtil.saveMasterPassword(masterPassword, MASTER_PASSWORD_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean loadMasterPassword() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(MASTER_PASSWORD_FILE))) {
            masterPassword = (String) in.readObject();
            return masterPassword != null;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    static class PasswordEntry implements Serializable {
		private static final long serialVersionUID = 1L;
		String site;
        String username;
        String password;

        PasswordEntry(String site, String username, String password) {
            this.site = site;
            this.username = username;
            this.password = password;
        }
    }
}

