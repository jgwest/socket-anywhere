/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.runner;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowListener;

import com.jsocksproxy.impl.InetRange;
import com.jsocksproxy.impl.Proxy;
import com.jsocksproxy.impl.Socks4Proxy;
import com.jsocksproxy.impl.Socks5Proxy;
import com.jsocksproxy.impl.UserPasswordAuthentication;

/**
 * Socks configuration dialog.<br>
 * Class which provides GUI means of getting Proxy configuration from the user.
 */
public class SocksDialog extends Dialog implements WindowListener, ItemListener, ActionListener, Runnable {

	private static final long serialVersionUID = 1L;
	
	// GUI components
	TextField _host_text, _port_text, _user_text, _password_text, _direct_text;
	Button _add_button, _remove_button, _cancel_button, _ok_button, _dismiss_button;
	List _direct_list;
	Checkbox _socks4radio, _socks5radio, _none_check, _up_check, _gssapi_check;

	Dialog _warning_dialog;
	Label _warning_label;

	String _host, _user, _password;
	int _port;
	Thread _net_thread = null;

	// CheckboxGroups
	CheckboxGroup _socks_group = new CheckboxGroup();

	Proxy _proxy;
	InetRange _ir;

	final static int COMMAND_MODE = 0;
	final static int OK_MODE = 1;

	int _mode;

	/**
	 * Wether to resolve addresses in separate thread.
	 * <p>
	 * Default value is true, however on some JVMs, namely one from the Microsoft, it doesn't want to work properly,
	 * separate thread can't close the dialog opened in GUI thread, and everuthing else is then crashes.
	 * <p>
	 * When setting this variable to false, SocksDialog will block while trying to look up proxy host, and if this takes
	 * considerable amount of time it might be annoying to user.
	 */
	public static boolean _useThreads = true;

	// Constructors
	// //////////////////////////////////
	/**
	 * Creates SOCKS configuration dialog.<br>
	 * Uses default initialisation:<br>
	 * Proxy host: socks-proxy <br>
	 * Proxy port: 1080 <br>
	 * Version: 5<br>
	 */
	public SocksDialog(Frame parent) {
		this(parent, null);
	}

	/**
	 * Creates SOCKS configuration dialog and initialises it to given proxy.
	 */
	public SocksDialog(Frame parent, Proxy init_proxy) {
		super(parent, "Proxy Configuration", true);
		_warning_dialog = new Dialog(parent, "Warning", true);

		guiInit();
		setResizable(false);
		addWindowListener(this);
		Component[] comps = getComponents();
		for (int i = 0; i < comps.length; ++i) {
			if (comps[i] instanceof Button)
				((Button) comps[i]).addActionListener(this);
			else if (comps[i] instanceof TextField)
				((TextField) comps[i]).addActionListener(this);
			else if (comps[i] instanceof Checkbox) {
				((Checkbox) comps[i]).addItemListener(this);
			}
		}
		_proxy = init_proxy;
		if (_proxy != null)
			doInit(_proxy);
		else
			_ir = new InetRange();

		_dismiss_button.addActionListener(this);
		_warning_dialog.addWindowListener(this);
	}

	// Public Methods
	// //////////////

	/**
	 * Displays SOCKS configuartion dialog.
	 * <p>
	 * Returns initialised proxy object, or null if user cancels dialog by either pressing Cancel or closing the dialog
	 * window.
	 */
	@SuppressWarnings("deprecation")
	public Proxy getProxy() {
		_mode = COMMAND_MODE;
		pack();
		show();
		return _proxy;
	}

	/**
	 * Initialises dialog to given proxy and displays SOCKS configuartion dialog.
	 * <p>
	 * Returns initialised proxy object, or null if user cancels dialog by either pressing Cancel or closing the dialog
	 * window.
	 */
	@SuppressWarnings("deprecation")
	public Proxy getProxy(Proxy p) {
		if (p != null) {
			doInit(p);
		}
		_mode = COMMAND_MODE;
		pack();
		show();
		return _proxy;
	}

	// WindowListener Interface
	// ///////////////////////////////
	public void windowActivated(java.awt.event.WindowEvent e) {
	}

	public void windowDeactivated(java.awt.event.WindowEvent e) {
	}

	public void windowOpened(java.awt.event.WindowEvent e) {
	}

	public void windowClosing(java.awt.event.WindowEvent e) {
		Window source = e.getWindow();
		if (source == this) {
			onCancel();
		} else if (source == _warning_dialog) {
			onDismiss();
		}
	}

	public void windowClosed(java.awt.event.WindowEvent e) {
	}

	public void windowIconified(java.awt.event.WindowEvent e) {
	}

	public void windowDeiconified(java.awt.event.WindowEvent e) {
	}

	// ActionListener interface
	// /////////////////////////
	public void actionPerformed(ActionEvent ae) {

		Object source = ae.getSource();

		if (source == _cancel_button)
			onCancel();
		else if (source == _add_button || source == _direct_text)
			onAdd();
		else if (source == _remove_button)
			onRemove();
		else if (source == _dismiss_button)
			onDismiss();
		else if (source == _ok_button || source instanceof TextField)
			onOK();
	}

	// ItemListener interface
	// //////////////////////
	public void itemStateChanged(ItemEvent ie) {
		Object source = ie.getSource();
		// System.out.println("ItemEvent:"+source);
		if (source == _socks5radio || source == _socks4radio)
			onSocksChange();
		else if (source == _up_check)
			onUPChange();

	}

	// Runnable interface
	// //////////////////
	/**
	 * Resolves proxy address in other thread, to avoid annoying blocking in GUI thread.
	 */
	public void run() {

		if (!initProxy()) {
			// Check if we have been aborted
			if (_mode != OK_MODE)
				return;
			if (_net_thread != Thread.currentThread())
				return;

			_mode = COMMAND_MODE;
			_warning_label.setText("Look up failed.");
			_warning_label.invalidate();
			return;
		}

		// System.out.println("Done!");
		while (!_warning_dialog.isShowing())
			; /* do nothing */
		;

		_warning_dialog.dispose();
		// dispose(); //End Dialog
	}

	// Private Methods
	// /////////////////
	private void onOK() {
		_host = _host_text.getText().trim();
		_user = _user_text.getText();
		_password = _password_text.getText();

		if (_host.length() == 0) {
			warn("Proxy host is not set!");
			return;
		}
		if (_socks_group.getSelectedCheckbox() == _socks4radio) {
			if (_user.length() == 0) {
				warn("User name is not set");
				return;
			}

		} else {
			if (_up_check.getState()) {
				if (_user.length() == 0) {
					warn("User name is not set.");
					return;
				}
				if (_password.length() == 0) {
					warn("Password is not set.");
					return;
				}
			} else if (!_none_check.getState()) {
				warn("Please select at least one Authentication Method.");
				return;
			}
		}

		try {
			_port = Integer.parseInt(_port_text.getText());
		} catch (NumberFormatException nfe) {
			warn("Proxy port is invalid!");
			return;
		}

		_mode = OK_MODE;

		if (_useThreads) {
			_net_thread = new Thread(this);
			_net_thread.start();

			info("Looking up host: " + _host);
			// System.out.println("Info returned.");
		} else if (!initProxy()) {
			warn("Proxy host is invalid.");
			_mode = COMMAND_MODE;
		}

		if (_mode == OK_MODE)
			dispose();
	}

	private void onCancel() {
		// System.out.println("Cancel");
		_proxy = null;
		dispose();
	}

	private void onAdd() {
		String s = _direct_text.getText();
		s.trim();
		if (s.length() == 0)
			return;
		// Check for Duplicate
		String[] direct_hosts = _direct_list.getItems();
		for (int i = 0; i < direct_hosts.length; ++i)
			if (direct_hosts[i].equals(s))
				return;

		_direct_list.add(s);
		_ir.add(s);
	}

	private void onRemove() {
		int index = _direct_list.getSelectedIndex();
		if (index < 0)
			return;
		_ir.remove(_direct_list.getItem(index));
		_direct_list.remove(index);
		_direct_list.select(index);
	}

	private void onSocksChange() {
		Object selected = _socks_group.getSelectedCheckbox();
		if (selected == _socks4radio) {
			_user_text.setEnabled(true);
			_password_text.setEnabled(false);
			_none_check.setEnabled(false);
			_up_check.setEnabled(false);
		} else {
			if (_up_check.getState()) {
				_user_text.setEnabled(true);
				_password_text.setEnabled(true);
			} else {
				_user_text.setEnabled(false);
				_password_text.setEnabled(false);
			}
			_none_check.setEnabled(true);
			_up_check.setEnabled(true);
		}
		// System.out.println("onSocksChange:"+selected);
	}

	private void onUPChange() {
		// System.out.println("onUPChange");
		if (_up_check.getState()) {
			_user_text.setEnabled(true);
			_password_text.setEnabled(true);
		} else {
			_user_text.setEnabled(false);
			_password_text.setEnabled(false);
		}
	}

	private void onDismiss() {
		_warning_dialog.dispose();
		if (_mode == OK_MODE) {
			_mode = COMMAND_MODE;
			if (_net_thread != null)
				_net_thread.interrupt();
		}
	}

	private void doInit(Proxy p) {
		if (p.getVersion() == 5) {
			_socks_group.setSelectedCheckbox(_socks5radio);
			onSocksChange();
			if (((Socks5Proxy) p).getAuthenticationMethod(0) != null)
				_none_check.setState(true);
			UserPasswordAuthentication auth = (UserPasswordAuthentication) ((Socks5Proxy) p).getAuthenticationMethod(2);
			if (auth != null) {
				_user_text.setText(auth.getUser());
				_password_text.setText(auth.getPassword());
				_up_check.setState(true);
				onUPChange();
			}
		} else {
			_socks_group.setSelectedCheckbox(_socks4radio);
			onSocksChange();
			_user_text.setText(((Socks4Proxy) p).getUser());
		}
		_ir = (InetRange) (p.getDirect().clone());
		String[] direct_hosts = _ir.getAll();
		_direct_list.removeAll();
		for (int i = 0; i < direct_hosts.length; ++i)
			_direct_list.add(direct_hosts[i]);

		_host_text.setText(p.getInetAddress().getHostName());
		_port_text.setText("" + p.getPort());

	}

	private boolean initProxy() {
		try {
			if (_socks_group.getSelectedCheckbox() == _socks5radio) {
				_proxy = new Socks5Proxy(_host, _port);
				if (_up_check.getState())
					((Socks5Proxy) _proxy).setAuthenticationMethod(2, new UserPasswordAuthentication(_user, _password));
				if (!_none_check.getState())
					((Socks5Proxy) _proxy).setAuthenticationMethod(0, null);
			} else
				_proxy = new Socks4Proxy(_host, _port, _user);
		} catch (java.net.UnknownHostException uhe) {
			return false;
		}
		_proxy.setDirect(_ir);
		return true;
	}

	private void info(String s) {
		msgBox("Info", s);
	}

	private void warn(String s) {
		msgBox("Warning", s);
	}

	@SuppressWarnings("deprecation")
	private void msgBox(String title, String message) {
		_warning_label.setText(message);
		_warning_label.invalidate();
		_warning_dialog.setTitle(title);
		_warning_dialog.pack();
		_warning_dialog.show();
	}

	/*
	 * ====================================================================== Form: Table: +---+-------+---+---+ | | | |
	 * | +---+-------+---+---+ | | | +---+-------+-------+ | | | | +---+-------+-------+ | | | | +---+-------+-------+ |
	 * | | +-----------+-------+ | | | | +---+---+ | | | | +-----------+---+---+ | | | | +---+---+---+---+---+ | | | | |
	 * | +---+---+---+---+---+
	 */

	void guiInit() {
		// Some default names used
		Label label;
		Container container;
		Font font;

		GridBagConstraints c = new GridBagConstraints();

		font = new Font("Dialog", Font.PLAIN, 12);

		container = this;
		// container = new Panel();
		container.setLayout(new GridBagLayout());
		container.setFont(font);
		container.setBackground(SystemColor.menu);
		c.insets = new Insets(3, 3, 3, 3);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHEAST;
		label = new Label("Host:");
		container.add(label, c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_host_text = new TextField("socks-proxy", 15);
		container.add(_host_text, c);

		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHEAST;
		label = new Label("Port:");
		container.add(label, c);

		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_port_text = new TextField("1080", 5);
		container.add(_port_text, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		_socks4radio = new Checkbox("Socks4", _socks_group, false);
		// 1.0 compatible code
		// socks4radio = new Checkbox("Socks4",false);
		// socks4radio.setCheckboxGroup(socks_group);
		_socks4radio.setFont(new Font(font.getName(), Font.BOLD, 14));
		container.add(_socks4radio, c);

		c.gridx = 3;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		_socks5radio = new Checkbox("Socks5", _socks_group, true);
		// 1.0 compatible code
		// socks5radio = new Checkbox("Socks5",true);
		// socks5radio.setCheckboxGroup(socks_group);
		_socks5radio.setFont(new Font(font.getName(), Font.BOLD, 14));
		container.add(_socks5radio, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.EAST;
		label = new Label("User Id:");
		container.add(label, c);

		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_user_text = new TextField("", 15);
		_user_text.setEnabled(false);
		container.add(_user_text, c);

		c.gridx = 3;
		c.gridy = 2;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		label = new Label("Authentication");
		label.setFont(new Font(font.getName(), Font.BOLD, 14));
		container.add(label, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.EAST;
		label = new Label("Password:");
		container.add(label, c);

		c.gridx = 1;
		c.gridy = 3;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_password_text = new TextField("", 15);
		_password_text.setEchoChar('*');
		_password_text.setEnabled(false);
		// password_text.setEchoCharacter('*');//1.0
		container.add(_password_text, c);

		c.gridx = 3;
		c.gridy = 3;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_none_check = new Checkbox("None", true);
		container.add(_none_check, c);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 3;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		label = new Label("Direct Hosts");
		label.setFont(new Font(font.getName(), Font.BOLD, 14));
		container.add(label, c);

		c.gridx = 3;
		c.gridy = 4;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_up_check = new Checkbox("User/Password", false);
		container.add(_up_check, c);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 3;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		_direct_list = new List(3);
		container.add(_direct_list, c);

		c.gridx = 3;
		c.gridy = 5;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_gssapi_check = new Checkbox("GSSAPI", false);
		_gssapi_check.setEnabled(false);
		container.add(_gssapi_check, c);

		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 3;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_direct_text = new TextField("", 25);
		container.add(_direct_text, c);

		c.gridx = 3;
		c.gridy = 7;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		_add_button = new Button("Add");
		container.add(_add_button, c);

		c.gridx = 3;
		c.gridy = 6;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		_remove_button = new Button("Remove");
		container.add(_remove_button, c);

		c.gridx = 1;
		c.gridy = 8;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTH;
		_cancel_button = new Button("Cancel");
		container.add(_cancel_button, c);

		c.gridx = 0;
		c.gridy = 8;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		_ok_button = new Button("OK");
		container.add(_ok_button, c);

		// up_check.setEnabled(false);

		// Warning Dialog
		_dismiss_button = new Button("Dismiss");
		_warning_label = new Label("", Label.CENTER);
		_warning_label.setFont(new Font("Dialog", Font.BOLD, 15));

		Panel p = new Panel();
		p.add(_dismiss_button);
		_warning_dialog.add(p, BorderLayout.SOUTH);
		_warning_dialog.add(_warning_label, BorderLayout.CENTER);
		_warning_dialog.setResizable(false);
	}// end guiInit

	/*
	 * // Main //////////////////////////////////// public static void main(String[] args) throws Exception{ Frame f =
	 * new Frame("Test for SocksDialog"); f.add("Center", new Label("Fill the Dialog")); SocksDialog socksdialog = new
	 * SocksDialog(f); f.pack(); f.show(); f.addWindowListener(socksdialog); Proxy p = socksdialog.getProxy();
	 * System.out.println("Selected: "+p); }
	 */

}// end class
