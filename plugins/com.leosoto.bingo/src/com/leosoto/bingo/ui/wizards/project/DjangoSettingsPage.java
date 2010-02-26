package com.leosoto.bingo.ui.wizards.project;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DjangoSettingsPage extends WizardPage {

	public static final String CPYTHON = "cpython";
	public static final String JYTHON = "jython";
	public static final String POSTGRESQL = "PostgreSQL";
	public static final String SQLITE= "SQLite";
	public static final String MYSQL= "MySQL";
	public static final String ORACLE= "Oracle";

	static final Map<String, Map<String, String>> DB_ENGINES = new HashMap<String, Map<String, String>>() {{
		put(CPYTHON, new HashMap<String, String>() {{
			put(POSTGRESQL, "postgresql_psycopg2");
			put(SQLITE, "sqlite3");
			put(MYSQL, "mysql");
			put(ORACLE, "oracle");
		}});
		put(JYTHON, new HashMap<String, String>() {{
			put(POSTGRESQL, "doj.backends.zxjdbc.postgresql");
			put(SQLITE, "doj.backends.zxjdbc.sqlite3");
			put(MYSQL, "doj.backends.zxjdbc.mysql");
			put(ORACLE, "doj.backends.zxjdbc.oracle");
		}});
	}};


	private static final int SIZING_TEXT_FIELD_WIDTH = 250;

    private Combo engineCombo;
    private Text nameText;
    private Text hostText;
    private Text portText;
    private Text userText;
    private Text passText;

    public DjangoSettingsPage(String pageName) {
        super(pageName);
        setTitle("Django Settings");
        setDescription("Basic Django Settings");
    }


    private Label newLabel(Composite parent, String label) {
    	Label l = new Label(parent, SWT.NONE);
    	l.setText(label);
    	l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	return l;
    }

    private Text newText(Composite parent) {
    	Text t = new Text(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
        t.setLayoutData(gd);
        return t;
    }

	@Override
	public void createControl(Composite parent) {
        Composite topComp= new Composite(parent, SWT.NONE);
        GridLayout innerLayout= new GridLayout();
        innerLayout.numColumns= 1;
        innerLayout.marginHeight= 0;
        innerLayout.marginWidth= 0;
        topComp.setLayout(innerLayout);
        GridData gd= new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);

        //Database Settings
        Group group = new Group(topComp, SWT.NONE);
        group.setText("Database settings");
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.numColumns = 2;
        group.setLayout(layout);
        gd= new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);

        // Database Engine
        Label engineLabel = newLabel(group, "Database &Engine");

        engineCombo = new Combo(group, SWT.READ_ONLY);
        engineCombo.add(POSTGRESQL);
        engineCombo.add(SQLITE);
        engineCombo.add(MYSQL);
        engineCombo.add(ORACLE);
        engineCombo.setText(POSTGRESQL);
        gd= new GridData(GridData.FILL_HORIZONTAL);
        engineCombo.setLayoutData(gd);

        // Database Name
        Label nameLabel = newLabel(group, "Database &Name");
        nameText = newText(group);
        // Database Host
        Label hostLabel = newLabel(group, "Database &Host");
        hostText = newText(group);
        // Database Port
        Label portLabel = newLabel(group, "Database P&ort");
        portText = newText(group);

        // Database User
        Label userLabel = newLabel(group, "&Username");
        userText = newText(group);
        // Database Pass
        Label passLabel = newLabel(group, "&Password");
        passText = newText(group);
        passText.setEchoChar('*');
        setErrorMessage(null);
        setMessage(null);
        setControl(topComp);
	}

	public static class DjangoSettings {
		public String databaseEngine;
		public String databaseName;
		public String databaseHost;
		public String databasePort;
		public String databaseUser;
		public String databasePassword;

	}

	public DjangoSettings getSettings(String configuration) {
		DjangoSettings s = new DjangoSettings();
		s.databaseEngine = DB_ENGINES.get(configuration).get(engineCombo.getText());
		s.databaseName = nameText.getText();
		s.databaseHost = hostText.getText();
		s.databasePort = portText.getText();
		s.databaseUser = userText.getText();
		s.databasePassword = passText.getText();
		return s;
	}
}
