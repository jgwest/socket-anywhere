/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.test;
import java.net.Socket;

import com.jsocksproxy.authentication.IUserValidation;
import com.jsocksproxy.authentication.UserPasswordAuthenticator;
import com.jsocksproxy.impl.ProxyServer;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;


/** Test file for UserPasswordAuthentictor */

public class UPSOCKS implements IUserValidation{
    String _user, _password;

    UPSOCKS(String user,String password){
       this._user = user;
       this._password = password;
    }

    public boolean isUserValid(String user,String password,Socket s){
       System.err.println("User:"+user+"\tPassword:"+password);
       System.err.println("Socket:"+s);
       
       return (user.equals(this._user) && password.equals(this._password));
    }

    public static void main(String args[]){
        String user, password;

        if(args.length == 2){
          user = args[0];
          password = args[1];
        }else{
          user = "user";
          password = "password";
        }

        UPSOCKS us = new UPSOCKS(user,password);
        UserPasswordAuthenticator auth = new UserPasswordAuthenticator(us);
        ProxyServer server = new ProxyServer(auth);

        server.setLog(System.out);
        server.start(new TLAddress(1080));
    }

	@Override
	public boolean isUserValid(String username, String password, ISocketTL connection) {
		return true;
	}
}
